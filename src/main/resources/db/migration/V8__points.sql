-- Sistema de pontos (escopo 8, req. 36–37). O cliente ganha pontos quando o pedido é concluído e
-- os usa como desconto em pedidos futuros. points_transaction é o extrato e a fonte da verdade;
-- client.promotion_points passa a ser o saldo em cache, atualizado na mesma transação.

-- Configuração única (id = 1): quantos pontos vale R$1 pago, quanto vale um ponto no resgate e por
-- quantos dias os pontos ganhos valem (nulo = não expiram).
create table points_settings (
    id                 bigint        not null primary key check (id = 1),
    earn_rate          numeric(10, 4) not null check (earn_rate >= 0),
    point_value        numeric(38, 2) not null check (point_value > 0),
    expiration_days    integer       check (expiration_days > 0),
    updated_at         timestamptz   not null,
    updated_by_user_id bigint        constraint fk_points_settings_user references "user" on delete set null
);

insert into points_settings (id, earn_rate, point_value, expiration_days, updated_at)
values (1, 1, 0.05, 365, now());

create sequence points_transaction_seq start with 1 increment by 50;

-- Toda linha positiva (EARN, ADJUST positivo) é um lote: remaining é o que ainda pode ser gasto e
-- expires_at, quando vence. Gastos, estornos e expirações apontam o lote afetado em lot_id.
create table points_transaction (
    id                 bigint      not null primary key,
    client_id          bigint      not null constraint fk_points_transaction_client references client on delete cascade,
    order_id           bigint      constraint fk_points_transaction_order references orders on delete set null,
    type               varchar(20) not null check (type in ('EARN', 'REDEEM', 'REFUND', 'EXPIRE', 'ADJUST')),
    points             integer     not null check (points <> 0),
    remaining          integer     check (remaining >= 0),
    expires_at         timestamptz,
    lot_id             bigint      constraint fk_points_transaction_lot references points_transaction on delete cascade,
    reason             varchar(500),
    created_by_user_id bigint      constraint fk_points_transaction_user references "user" on delete set null,
    created_at         timestamptz not null
);

create index ix_points_transaction_client on points_transaction (client_id);
create index ix_points_transaction_order on points_transaction (order_id);

-- Snapshot das taxas no checkout: mudar a configuração não altera pedidos já feitos.
alter table orders
    add column points_redeemed integer        not null default 0 check (points_redeemed >= 0),
    add column point_value     numeric(38, 2),
    add column earn_rate       numeric(10, 4),
    add column points_earned   integer        not null default 0 check (points_earned >= 0);

-- Item de promoção com enable_promotion_points = false não entra na base do resgate.
alter table order_item
    add column points_eligible boolean not null default true;

update client set promotion_points = 0 where promotion_points is null;
alter table client
    alter column promotion_points set default 0,
    alter column promotion_points set not null;

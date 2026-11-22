-- Pagamento online com o Mercado Pago (docs/payments.md). O pedido ganha o momento do pagamento
-- (online ou presencial) e o status AWAITING_PAYMENT, anterior a PENDING. Cada tentativa de
-- cobrança é um payment; ele sobrevive ao pedido (restrict) porque é registro financeiro.

alter table orders add column payment_channel varchar(20) not null default 'IN_PERSON'
    check (payment_channel in ('ONLINE', 'IN_PERSON'));
alter table orders alter column payment_channel drop default;

alter table orders drop constraint orders_status_check;
alter table orders add constraint orders_status_check
    check (status in ('AWAITING_PAYMENT', 'PENDING', 'IN_PREPARATION', 'READY', 'OUT_FOR_DELIVERY',
                      'COMPLETED', 'CANCELLED'));

alter table order_status_history drop constraint order_status_history_status_check;
alter table order_status_history add constraint order_status_history_status_check
    check (status in ('AWAITING_PAYMENT', 'PENDING', 'IN_PREPARATION', 'READY', 'OUT_FOR_DELIVERY',
                      'COMPLETED', 'CANCELLED'));

create sequence payment_seq start with 1 increment by 50;

create table payment (
    id                 bigint         not null primary key,
    order_id           bigint         not null constraint fk_payment_order references orders on delete restrict,
    provider           varchar(20)    not null check (provider in ('MERCADO_PAGO')),
    external_id        varchar(64)    constraint uk_payment_external_id unique,
    idempotency_key    varchar(64)    not null constraint uk_payment_idempotency_key unique,
    method             varchar(20)    not null check (method in ('PIX', 'CARD')),
    status             varchar(20)    not null check (status in ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED',
                                                                 'CANCELLED', 'REFUNDED', 'CHARGED_BACK')),
    status_detail      varchar(100),
    amount             numeric(38, 2) not null,
    payer_email        varchar(255)   not null,
    pix_qr_code        text,
    pix_qr_code_base64 text,
    expires_at         timestamptz,
    approved_at        timestamptz,
    created_at         timestamptz    not null,
    updated_at         timestamptz    not null
);

create index ix_payment_order on payment (order_id);
-- Uma tentativa aberta e no máximo uma aprovada por pedido, mesmo com requests concorrentes.
create unique index uk_payment_order_pending on payment (order_id) where status = 'PENDING';
create unique index uk_payment_order_approved on payment (order_id) where status = 'APPROVED';

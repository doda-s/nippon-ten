-- Pedidos (docs/orders.md). Itens, adicionais, removidos, componentes de combo e histórico são
-- filhos do agregado: somem junto com o pedido. As referências ao catálogo e aos usuários viram
-- nulas se o item for excluído, porque o pedido guarda cópia de nome e preço e o histórico precisa
-- sobreviver. A tabela se chama orders porque order é palavra reservada.

create sequence orders_seq start with 1 increment by 50;
create sequence order_item_seq start with 1 increment by 50;
create sequence order_item_additional_seq start with 1 increment by 50;
create sequence order_item_removed_ingredient_seq start with 1 increment by 50;
create sequence order_item_combo_component_seq start with 1 increment by 50;
create sequence order_status_history_seq start with 1 increment by 50;

create table orders (
    id                     bigint       not null primary key,
    code                   varchar(16)  not null constraint uk_orders_code unique,
    client_id              bigint       constraint fk_orders_client references client on delete set null,
    created_by_internal_id bigint       constraint fk_orders_internal references internal on delete set null,
    tracking_token_hash    varchar(64)  constraint uk_orders_tracking_token_hash unique,
    guest_name             varchar(120),
    guest_phone            varchar(30),
    fulfillment_type       varchar(20)  not null check (fulfillment_type in ('DELIVERY', 'PICKUP', 'DINE_IN')),
    status                 varchar(20)  not null check (status in ('PENDING', 'IN_PREPARATION', 'READY', 'OUT_FOR_DELIVERY', 'COMPLETED', 'CANCELLED')),
    payment_method         varchar(20)  not null check (payment_method in ('PIX', 'CARD', 'CASH')),
    paid                   boolean      not null default false,
    paid_at                timestamptz,
    street_address         varchar(255),
    number                 varchar(255),
    cep                    varchar(255),
    complement             varchar(255),
    notes                  varchar(500),
    cancel_reason          varchar(500),
    subtotal               numeric(38, 2) not null,
    discount               numeric(38, 2) not null,
    delivery_fee           numeric(38, 2) not null,
    total                  numeric(38, 2) not null,
    created_at             timestamptz  not null,
    updated_at             timestamptz  not null
);

create index ix_orders_client on orders (client_id);
create index ix_orders_status on orders (status);

create table order_item (
    id                  bigint        not null primary key,
    order_id            bigint        not null constraint fk_order_item_order references orders on delete cascade,
    item_type           varchar(20)   not null check (item_type in ('PRODUCT', 'COMBO')),
    product_size_id     bigint        constraint fk_order_item_product_size references product_size on delete set null,
    combo_id            bigint        constraint fk_order_item_combo references combo on delete set null,
    promotion_id        bigint        constraint fk_order_item_promotion references promotion on delete set null,
    name                varchar(255)  not null,
    size_name           varchar(255),
    quantity            integer       not null check (quantity > 0),
    original_unit_price numeric(38, 2) not null,
    unit_price          numeric(38, 2) not null,
    total               numeric(38, 2) not null,
    notes               varchar(500)
);

create index ix_order_item_order on order_item (order_id);

create table order_item_additional (
    id            bigint        not null primary key,
    order_item_id bigint        not null constraint fk_order_item_additional_item references order_item on delete cascade,
    ingredient_id bigint        constraint fk_order_item_additional_ingredient references ingredient on delete set null,
    name          varchar(255)  not null,
    quantity      integer       not null check (quantity > 0),
    unit_price    numeric(38, 2) not null
);

create table order_item_removed_ingredient (
    id            bigint       not null primary key,
    order_item_id bigint       not null constraint fk_order_item_removed_item references order_item on delete cascade,
    ingredient_id bigint       constraint fk_order_item_removed_ingredient references ingredient on delete set null,
    name          varchar(255) not null
);

create table order_item_combo_component (
    id              bigint       not null primary key,
    order_item_id   bigint       not null constraint fk_order_item_component_item references order_item on delete cascade,
    product_id      bigint       constraint fk_order_item_component_product references product on delete set null,
    product_size_id bigint       constraint fk_order_item_component_product_size references product_size on delete set null,
    name            varchar(255) not null,
    size_name       varchar(255),
    quantity        integer      not null check (quantity > 0)
);

create table order_status_history (
    id                 bigint      not null primary key,
    order_id           bigint      not null constraint fk_order_status_history_order references orders on delete cascade,
    status             varchar(20) not null check (status in ('PENDING', 'IN_PREPARATION', 'READY', 'OUT_FOR_DELIVERY', 'COMPLETED', 'CANCELLED')),
    changed_at         timestamptz not null,
    changed_by_user_id bigint      constraint fk_order_status_history_user references "user" on delete set null
);

create index ix_order_status_history_order on order_status_history (order_id);

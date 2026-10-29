-- Schema inicial. Espelha as entidades JPA de infrastructure.entities; o Hibernate só valida.
-- As sequences usam incremento 50 porque é o allocationSize padrão de @GeneratedValue.

create sequence additional_ingredients_seq start with 1 increment by 50;
create sequence client_seq start with 1 increment by 50;
create sequence combo_product_seq start with 1 increment by 50;
create sequence combo_seq start with 1 increment by 50;
create sequence ingredient_seq start with 1 increment by 50;
create sequence internal_role_seq start with 1 increment by 50;
create sequence internal_seq start with 1 increment by 50;
create sequence product_ingredient_seq start with 1 increment by 50;
create sequence product_seq start with 1 increment by 50;
create sequence product_size_seq start with 1 increment by 50;
create sequence promotion_seq start with 1 increment by 50;
create sequence promotion_type_seq start with 1 increment by 50;
create sequence size_seq start with 1 increment by 50;
create sequence user_address_seq start with 1 increment by 50;
create sequence user_seq start with 1 increment by 50;

-- Usuários

create table "user" (
    id         bigint       not null primary key,
    email      varchar(255) not null unique,
    password   varchar(255),
    created_at timestamp(6),
    active     boolean      not null default true
);

create table client (
    id               bigint not null primary key,
    user_id          bigint constraint fk_client_user references "user",
    name             varchar(255),
    last_name        varchar(255),
    cpf              varchar(255),
    promotion_points integer
);

create table user_address (
    id             bigint not null primary key,
    client_id      bigint constraint fk_user_address_client references client,
    street_address varchar(255),
    number         varchar(255),
    cep            varchar(255),
    complement     varchar(255)
);

create table internal_role (
    id           bigint       not null primary key,
    name         varchar(255) not null unique,
    permissions  varchar(255) array,
    default_role boolean      not null default false,
    active       boolean      not null default true
);

create table internal (
    id               bigint not null primary key,
    user_id          bigint constraint fk_internal_user references "user",
    internal_role_id bigint constraint fk_internal_internal_role references internal_role,
    user_permissions varchar(255) array,
    name             varchar(255),
    last_name        varchar(255),
    cpf              varchar(255)
);

-- Catálogo

create table size (
    id   bigint       not null primary key,
    name varchar(255) not null
);

create table ingredient (
    id          bigint not null primary key,
    name        varchar(255),
    description varchar(255),
    image_url   varchar(255),
    price       numeric(38, 2),
    status      varchar(255) check (status in ('ACTIVE', 'OUT_OF_STOCK'))
);

create table product (
    id          bigint not null primary key,
    name        varchar(255),
    description varchar(255),
    image_url   varchar(255),
    status      varchar(255) check (status in ('ACTIVE', 'INACTIVE'))
);

create table product_size (
    id         bigint         not null primary key,
    product_id bigint constraint fk_product_size_product references product,
    size_id    bigint         not null constraint fk_product_size_size references size,
    price      numeric(38, 2) not null,
    status     varchar(255) check (status in ('ACTIVE', 'INACTIVE'))
);

create table product_ingredient (
    id            bigint not null primary key,
    product_id    bigint constraint fk_product_ingredient_product references product,
    ingredient_id bigint constraint fk_product_ingredient_ingredient references ingredient
);

create table additional_ingredients (
    id               bigint not null primary key,
    product_id       bigint constraint fk_additional_ingredients_product references product,
    ingredient_id    bigint constraint fk_additional_ingredients_ingredient references ingredient,
    maximum_quantity integer,
    status           varchar(255) check (status in ('ACTIVE', 'INACTIVE'))
);

-- Combos e promoções

create table combo (
    id          bigint not null primary key,
    name        varchar(255),
    description varchar(255),
    image_url   varchar(255),
    price       numeric(38, 2),
    start_date  timestamp(6),
    end_date    timestamp(6),
    status      varchar(255) check (status in ('ACTIVE', 'INACTIVE'))
);

create table combo_product (
    id         bigint not null primary key,
    combo_id   bigint constraint fk_combo_product_combo references combo,
    product_id bigint constraint fk_combo_product_product references product
);

create table promotion_type (
    id          bigint not null primary key,
    name        varchar(255),
    description varchar(255),
    type        varchar(255) check (type in ('PERCENTAGE_DISCOUNT', 'FIXED_DISCOUNT')),
    value       numeric(38, 2)
);

create table promotion (
    id                      bigint  not null primary key,
    product_id              bigint constraint fk_promotion_product references product,
    promotion_type_id       bigint constraint fk_promotion_promotion_type references promotion_type,
    title                   varchar(255),
    description             varchar(255),
    image_url               varchar(255),
    start_date              timestamp(6),
    end_date                timestamp(6),
    enable_promotion_points boolean not null,
    status                  varchar(255) check (status in ('ACTIVE', 'INACTIVE'))
);

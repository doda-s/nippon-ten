-- Categorias do cardápio (Pastéis, Sucos, Pizzas...). O produto aponta para uma categoria, que é
-- opcional: os produtos de antes ficam sem categoria até alguém escolher uma.
create sequence category_seq start with 1 increment by 50;

create table category (
    id            bigint      not null primary key,
    name          varchar(60) not null,
    display_order integer     not null default 0
);

-- Nome único sem diferenciar maiúsculas, a mesma regra que o service confere antes de gravar.
create unique index uk_category_name on category (lower(name));

alter table product add column category_id bigint constraint fk_product_category references category;

create index ix_product_category on product (category_id);

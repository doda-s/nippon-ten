-- Uma promoção passa a valer para vários produtos e/ou combos. As listas são sempre substituídas
-- inteiras junto com a promoção, então as tabelas de junção não têm id próprio.
create table promotion_product (
    promotion_id bigint not null constraint fk_promotion_product_promotion references promotion,
    product_id   bigint not null constraint fk_promotion_product_product references product,
    primary key (promotion_id, product_id)
);

create table promotion_combo (
    promotion_id bigint not null constraint fk_promotion_combo_promotion references promotion,
    combo_id     bigint not null constraint fk_promotion_combo_combo references combo,
    primary key (promotion_id, combo_id)
);

insert into promotion_product (promotion_id, product_id)
select id, product_id from promotion where product_id is not null;

alter table promotion drop column product_id;

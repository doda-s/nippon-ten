-- O combo passa a dizer o tamanho de cada produto que o compõe (o tamanho vive em product_size)
-- e a quantidade, para "2x o mesmo pastel" virar uma linha só. O tamanho é opcional porque um
-- produto sem tamanho (sem preço próprio) ainda pode entrar num combo. O preço cobrado continua
-- sendo o do combo.

alter table combo_product
    add column product_size_id bigint constraint fk_combo_product_product_size references product_size;
alter table combo_product
    add column quantity integer not null default 1 constraint ck_combo_product_quantity check (quantity > 0);

-- Combos existentes ficam sem tamanho, para revisão manual: o sistema não escolhe um por conta
-- própria. O mesmo produto repetido no combo vira uma linha com a quantidade somada.
update combo_product cp
set quantity = dup.total
from (select min(id) as keep_id, count(*) as total
      from combo_product
      group by combo_id, product_id
      having count(*) > 1) dup
where cp.id = dup.keep_id;

delete from combo_product cp
using combo_product other
where cp.combo_id = other.combo_id
  and cp.product_id = other.product_id
  and cp.id > other.id;

alter table combo_product alter column quantity drop default;
alter table combo_product alter column product_id set not null;

-- A mesma combinação produto+tamanho só aparece uma vez por combo. Dois índices parciais porque
-- um unique comum trataria tamanhos nulos como sempre diferentes.
create unique index uk_combo_product_with_size
    on combo_product (combo_id, product_id, product_size_id) where product_size_id is not null;
create unique index uk_combo_product_without_size
    on combo_product (combo_id, product_id) where product_size_id is null;

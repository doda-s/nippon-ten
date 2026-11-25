-- E-mail do visitante, para onde vai o link de acompanhamento (docs/orders.md). Obrigatório no
-- pedido que o próprio visitante faz e opcional no que o funcionário lança, por isso nulo no banco.
-- Pedidos antigos ficam sem e-mail.
alter table orders add column guest_email varchar(255);

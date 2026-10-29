-- Avaliação do pedido pelo cliente depois da entrega (req. 48). Um feedback por pedido, que some
-- junto com o pedido.

create sequence order_feedback_seq start with 1 increment by 50;

create table order_feedback (
    id         bigint        not null primary key,
    order_id   bigint        not null constraint uk_order_feedback_order unique
                             constraint fk_order_feedback_order references orders on delete cascade,
    rating     integer       not null check (rating between 1 and 5),
    comment    varchar(1000),
    created_at timestamptz   not null
);

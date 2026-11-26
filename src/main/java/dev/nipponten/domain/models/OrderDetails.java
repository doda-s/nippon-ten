package dev.nipponten.domain.models;

import java.util.List;

// feedback é nulo enquanto o pedido não foi avaliado. client é o dono do pedido, lido na hora (o
// pedido guarda só o id): nulo para visitante e para cliente que excluiu a conta.
public record OrderDetails(
        Order order,
        List<OrderStatusChange> statusHistory,
        OrderFeedback feedback,
        Client client) {}

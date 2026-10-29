package dev.nipponten.domain.models;

import java.util.List;

// feedback é nulo enquanto o pedido não foi avaliado.
public record OrderDetails(
        Order order, List<OrderStatusChange> statusHistory, OrderFeedback feedback) {}

package dev.nipponten.domain.models;

import java.util.List;

public record OrderDetails(Order order, List<OrderStatusChange> statusHistory) {}

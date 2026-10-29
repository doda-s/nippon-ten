package dev.nipponten.domain.models;

import java.time.Instant;

// changedByUserId é nulo quando quem mudou foi um visitante.
public record OrderStatusChange(
        Long id, Long orderId, Order.Status status, Instant changedAt, Long changedByUserId) {}

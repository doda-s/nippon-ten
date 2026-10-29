package dev.nipponten.domain.models;

import java.time.Instant;

// Aviso de que um pedido mudou. É só um aviso: o estado oficial é o pedido (GET /orders/{id}).
public record OrderEvent(
        Type type,
        Long orderId,
        String code,
        Order.Status status,
        boolean paid,
        Instant occurredAt) {

    public enum Type {
        CREATED,
        STATUS_CHANGED,
        PAID,
        CANCELLED,
        ITEMS_UPDATED
    }

    public static OrderEvent of(Type type, Order order) {
        return new OrderEvent(
                type, order.id(), order.code(), order.status(), order.paid(), order.updatedAt());
    }
}

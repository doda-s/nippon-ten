package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Order;
import dev.nipponten.domain.models.OrderEvent;
import java.time.Instant;

public record OrderEventResponse(
        OrderEvent.Type type,
        Long orderId,
        String code,
        Order.Status status,
        boolean paid,
        Instant occurredAt) {

    public static OrderEventResponse of(OrderEvent event) {
        return new OrderEventResponse(
                event.type(),
                event.orderId(),
                event.code(),
                event.status(),
                event.paid(),
                event.occurredAt());
    }
}

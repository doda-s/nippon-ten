package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Order;
import java.math.BigDecimal;
import java.time.Instant;

// Linha de listagem; os itens e o histórico ficam no detalhe (GET /orders/{id}).
public record OrderSummaryResponse(
        Long id,
        String code,
        Long clientId,
        String guestName,
        Order.FulfillmentType fulfillmentType,
        Order.Status status,
        Order.PaymentMethod paymentMethod,
        boolean paid,
        int itemCount,
        BigDecimal total,
        Instant createdAt) {

    public static OrderSummaryResponse of(Order order) {
        return new OrderSummaryResponse(
                order.id(),
                order.code(),
                order.clientId(),
                order.guestName(),
                order.fulfillmentType(),
                order.status(),
                order.paymentMethod(),
                order.paid(),
                order.items().stream().mapToInt(item -> item.quantity()).sum(),
                order.total(),
                order.createdAt());
    }
}

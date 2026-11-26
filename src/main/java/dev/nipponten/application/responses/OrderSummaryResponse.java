package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Client;
import dev.nipponten.domain.models.Order;
import dev.nipponten.domain.models.OrderSummary;
import java.math.BigDecimal;
import java.time.Instant;

// Linha de listagem; os itens e o histórico ficam no detalhe (GET /orders/{id}). clientName e
// clientPhone seguem OrderResponse.
public record OrderSummaryResponse(
        Long id,
        String code,
        Long clientId,
        String guestName,
        String clientName,
        String clientPhone,
        Order.FulfillmentType fulfillmentType,
        Order.Status status,
        Order.PaymentMethod paymentMethod,
        Order.PaymentChannel paymentChannel,
        boolean paid,
        int itemCount,
        BigDecimal total,
        Instant createdAt) {

    public static OrderSummaryResponse of(OrderSummary summary) {
        Order order = summary.order();
        Client client = summary.client();
        return new OrderSummaryResponse(
                order.id(),
                order.code(),
                order.clientId(),
                order.guestName(),
                client == null ? null : client.fullName(),
                client == null ? null : client.phone(),
                order.fulfillmentType(),
                order.status(),
                order.paymentMethod(),
                order.paymentChannel(),
                order.paid(),
                order.items().stream().mapToInt(item -> item.quantity()).sum(),
                order.total(),
                order.createdAt());
    }
}

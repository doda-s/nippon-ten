package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Order;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// Nunca expõe o hash do token de acompanhamento.
public record OrderResponse(
        Long id,
        String code,
        Long clientId,
        String guestName,
        String guestPhone,
        Order.FulfillmentType fulfillmentType,
        Order.Status status,
        Order.PaymentMethod paymentMethod,
        boolean paid,
        Instant paidAt,
        Address address,
        String notes,
        String cancelReason,
        List<OrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal deliveryFee,
        BigDecimal total,
        Instant createdAt,
        Instant updatedAt,
        List<StatusChange> statusHistory) {

    public record Address(String streetAddress, String number, String cep, String complement) {}

    public record StatusChange(Order.Status status, Instant changedAt) {}
}

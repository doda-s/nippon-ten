package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Order;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// Nunca expõe o hash do token de acompanhamento. clientName e clientPhone são os dados atuais do
// cliente dono do pedido (nulos para visitante, que usa guestName/guestPhone).
public record OrderResponse(
        Long id,
        String code,
        Long clientId,
        String guestName,
        String guestPhone,
        String guestEmail,
        String clientName,
        String clientPhone,
        Order.FulfillmentType fulfillmentType,
        Order.Status status,
        Order.PaymentMethod paymentMethod,
        Order.PaymentChannel paymentChannel,
        boolean paid,
        Instant paidAt,
        Address address,
        String notes,
        String cancelReason,
        List<OrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal deliveryFee,
        BigDecimal deliveryDistanceKm,
        BigDecimal total,
        int pointsRedeemed,
        int pointsEarned,
        Instant createdAt,
        Instant updatedAt,
        List<StatusChange> statusHistory,
        Feedback feedback) {

    public record Address(
            String streetAddress,
            String number,
            String cep,
            String complement,
            String neighborhood,
            String city,
            String state) {}

    public record StatusChange(Order.Status status, Instant changedAt) {}

    public record Feedback(int rating, String comment, Instant createdAt) {}
}

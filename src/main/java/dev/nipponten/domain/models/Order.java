package dev.nipponten.domain.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// O endereço é cópia do momento do pedido. clientId é nulo para visitante, que é identificado por
// guestName/guestPhone e acompanha o pedido pelo token (só o hash fica guardado).
public record Order(
        Long id,
        String code,
        Long clientId,
        Long createdByInternalId,
        String trackingTokenHash,
        String guestName,
        String guestPhone,
        FulfillmentType fulfillmentType,
        Status status,
        PaymentMethod paymentMethod,
        boolean paid,
        Instant paidAt,
        String streetAddress,
        String number,
        String cep,
        String complement,
        String notes,
        String cancelReason,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal deliveryFee,
        BigDecimal total,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItem> items) {

    public enum FulfillmentType {
        DELIVERY,
        PICKUP,
        DINE_IN
    }

    public enum Status {
        PENDING,
        IN_PREPARATION,
        READY,
        OUT_FOR_DELIVERY,
        COMPLETED,
        CANCELLED
    }

    public enum PaymentMethod {
        PIX,
        CARD,
        CASH
    }
}

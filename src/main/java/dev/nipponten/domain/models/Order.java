package dev.nipponten.domain.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// O endereço é cópia do momento do pedido. clientId é nulo para visitante, que é identificado por
// guestName/guestPhone e acompanha o pedido pelo token (só o hash fica guardado). guestEmail recebe
// o link de acompanhamento; só é nulo no pedido que o funcionário lança sem ele. pointValue e
// earnRate são cópias da configuração de pontos no checkout; nulos para visitante.
public record Order(
        Long id,
        String code,
        Long clientId,
        Long createdByInternalId,
        String trackingTokenHash,
        String guestName,
        String guestPhone,
        String guestEmail,
        FulfillmentType fulfillmentType,
        Status status,
        PaymentMethod paymentMethod,
        PaymentChannel paymentChannel,
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
        int pointsRedeemed,
        BigDecimal pointValue,
        BigDecimal earnRate,
        int pointsEarned,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItem> items) {

    public enum FulfillmentType {
        DELIVERY,
        PICKUP,
        DINE_IN
    }

    public enum Status {
        // Pedido online antes da confirmação do pagamento; a cozinha ainda não o vê.
        AWAITING_PAYMENT,
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

    // Quando o cliente paga: online pelo gateway ou presencialmente, na entrega ou no balcão.
    public enum PaymentChannel {
        ONLINE,
        IN_PERSON
    }
}

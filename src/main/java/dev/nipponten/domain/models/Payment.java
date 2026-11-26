package dev.nipponten.domain.models;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Uma tentativa de cobrança online de um pedido. externalId é o id do pagamento no gateway e fica
 * nulo só entre gravar a tentativa e receber a resposta; idempotencyKey é gerada antes da chamada
 * para uma nova tentativa depois de timeout não cobrar duas vezes.
 */
public record Payment(
        Long id,
        Long orderId,
        Provider provider,
        String externalId,
        String idempotencyKey,
        Method method,
        Status status,
        String statusDetail,
        BigDecimal amount,
        String payerEmail,
        String pixQrCode,
        String pixQrCodeBase64,
        Instant expiresAt,
        Instant approvedAt,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * statusDetail do pagamento aprovado de pedido cancelado cujo estorno o provedor recusou até
     * desistirmos. A reconciliação não tenta mais; alguém estorna pelo painel do provedor.
     */
    public static final String REFUND_REJECTED = "refund_rejected";

    public enum Provider {
        MERCADO_PAGO
    }

    public enum Method {
        PIX,
        CARD
    }

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED,
        EXPIRED,
        CANCELLED,
        REFUNDED,
        CHARGED_BACK
    }
}

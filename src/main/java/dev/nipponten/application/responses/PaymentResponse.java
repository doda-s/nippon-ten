package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Payment;
import java.math.BigDecimal;
import java.time.Instant;

// Sem e-mail do pagador, id externo nem chave de idempotência: são detalhes do provedor.
public record PaymentResponse(
        Long id,
        Payment.Method method,
        Payment.Status status,
        String statusDetail,
        BigDecimal amount,
        String pixQrCode,
        String pixQrCodeBase64,
        Instant expiresAt,
        Instant approvedAt,
        Instant createdAt) {

    public static PaymentResponse of(Payment payment) {
        return new PaymentResponse(
                payment.id(),
                payment.method(),
                payment.status(),
                payment.statusDetail(),
                payment.amount(),
                payment.pixQrCode(),
                payment.pixQrCodeBase64(),
                payment.expiresAt(),
                payment.approvedAt(),
                payment.createdAt());
    }
}

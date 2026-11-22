package dev.nipponten.domain.gateways;

import dev.nipponten.domain.models.Payment;
import java.math.BigDecimal;
import java.time.Duration;

/**
 * Porta para o provedor de pagamento. Não carrega nada específico do Mercado Pago, para trocar de
 * provedor sem mexer nas regras. Falhas de comunicação viram {@code PaymentGatewayException}.
 */
public interface PaymentGateway {

    Payment.Provider provider();

    /** Cria a cobrança Pix. Chamar de novo com a mesma idempotencyKey não cobra duas vezes. */
    Charge createPix(PixCharge charge);

    /** Estado atual no provedor, que é a fonte da verdade. */
    Charge fetch(String externalId);

    record PixCharge(
            String idempotencyKey,
            String reference,
            BigDecimal amount,
            String payerEmail,
            String payerName,
            Duration expiration) {}

    /** qrCode e qrCodeBase64 só vêm no Pix. */
    record Charge(
            String externalId,
            Payment.Status status,
            String statusDetail,
            String qrCode,
            String qrCodeBase64) {}
}

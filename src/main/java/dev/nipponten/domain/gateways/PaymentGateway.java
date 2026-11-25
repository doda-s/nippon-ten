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

    /**
     * Cobra o cartão tokenizado pelo front, à vista. O resultado já vem na resposta (aprovado,
     * recusado ou em processamento). Mesma idempotência do Pix.
     */
    Charge chargeCard(CardCharge charge);

    /** Estado atual no provedor, que é a fonte da verdade. */
    Charge fetch(String externalId);

    /**
     * Cancela uma cobrança ainda não paga. Se o provedor não puder cancelar (já paga, já vencida,
     * cartão em análise), devolve o estado atual em vez de falhar.
     */
    Charge cancel(String externalId, String idempotencyKey);

    /**
     * Estorna o valor total de uma cobrança paga. Se ela já foi estornada, ou não pode ser, devolve
     * o estado atual; a falha de comunicação vira {@code PaymentGatewayException}.
     */
    Charge refund(String externalId, String idempotencyKey);

    record PixCharge(
            String idempotencyKey,
            String reference,
            BigDecimal amount,
            String payerEmail,
            String payerName,
            Duration expiration) {}

    /**
     * {@code cardToken} e {@code cardBrand} vêm do formulário do provedor no front; o número do
     * cartão nunca passa pelo backend. A identificação (CPF ou CNPJ) é opcional.
     */
    record CardCharge(
            String idempotencyKey,
            String reference,
            BigDecimal amount,
            String payerEmail,
            String payerName,
            String identificationType,
            String identificationNumber,
            String cardToken,
            String cardBrand) {}

    /** qrCode e qrCodeBase64 só vêm no Pix. externalId é nulo se o provedor recusou sem criar. */
    record Charge(
            String externalId,
            Payment.Status status,
            String statusDetail,
            String qrCode,
            String qrCodeBase64) {}
}

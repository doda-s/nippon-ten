package dev.nipponten.infrastructure.payments.mercadopago;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.nipponten.application.exceptions.PaymentGatewayException;
import dev.nipponten.domain.gateways.PaymentGateway;
import dev.nipponten.domain.models.Payment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * Adapter do Mercado Pago pela API Orders. Cada Payment nosso é uma order deles com uma única
 * transação, então o status da order é o status do pagamento.
 */
@ApplicationScoped
public class MercadoPagoPaymentGateway implements PaymentGateway {

    private static final Logger LOG = Logger.getLogger(MercadoPagoPaymentGateway.class);

    @Inject @RestClient MercadoPagoClient client;

    @Inject ObjectMapper json;

    // Opcional para a aplicação subir sem credenciais (dev sem .env); a cobrança falha com 502.
    @ConfigProperty(name = "nippon.payments.mercadopago.access-token")
    Optional<String> accessToken;

    @Override
    public Payment.Provider provider() {
        return Payment.Provider.MERCADO_PAGO;
    }

    @Override
    public Charge createPix(PixCharge charge) {
        String amount = charge.amount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        MercadoPagoOrder.Create body =
                new MercadoPagoOrder.Create(
                        "online",
                        amount,
                        charge.reference(),
                        "automatic",
                        new MercadoPagoOrder.CreateTransactions(
                                List.of(
                                        new MercadoPagoOrder.CreatePayment(
                                                amount,
                                                new MercadoPagoOrder.CreatePaymentMethod(
                                                        "pix", "bank_transfer", null, null),
                                                charge.expiration().toString()))),
                        new MercadoPagoOrder.Payer(charge.payerEmail(), charge.payerName(), null));
        return call(() -> client.create(authorization(), charge.idempotencyKey(), body));
    }

    // Sempre à vista (docs/payments.md, Decisões).
    @Override
    public Charge chargeCard(CardCharge charge) {
        String amount = charge.amount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        MercadoPagoOrder.Create body =
                new MercadoPagoOrder.Create(
                        "online",
                        amount,
                        charge.reference(),
                        "automatic",
                        new MercadoPagoOrder.CreateTransactions(
                                List.of(
                                        new MercadoPagoOrder.CreatePayment(
                                                amount,
                                                new MercadoPagoOrder.CreatePaymentMethod(
                                                        charge.cardBrand(),
                                                        "credit_card",
                                                        charge.cardToken(),
                                                        1),
                                                null))),
                        new MercadoPagoOrder.Payer(
                                charge.payerEmail(),
                                charge.payerName(),
                                charge.identificationType() == null
                                        ? null
                                        : new MercadoPagoOrder.Identification(
                                                charge.identificationType(),
                                                charge.identificationNumber())));
        return call(() -> client.create(authorization(), charge.idempotencyKey(), body));
    }

    @Override
    public Charge fetch(String externalId) {
        return call(() -> client.get(authorization(), externalId));
    }

    @Override
    public Charge cancel(String externalId, String idempotencyKey) {
        return orCurrentState(
                externalId, () -> client.cancel(authorization(), idempotencyKey, externalId));
    }

    @Override
    public Charge refund(String externalId, String idempotencyKey) {
        return orCurrentState(
                externalId, () -> client.refund(authorization(), idempotencyKey, externalId));
    }

    // 409: a order não está num estado que aceita a operação (cannot_cancel_order,
    // order_already_refunded...). Quem chamou decide a partir do estado atual.
    private Charge orCurrentState(String externalId, Supplier<MercadoPagoOrder> request) {
        try {
            return call(request);
        } catch (PaymentGatewayException e) {
            if (e.getCause() instanceof WebApplicationException web
                    && web.getResponse().getStatus() == 409) {
                return fetch(externalId);
            }
            throw e;
        }
    }

    private String authorization() {
        return "Bearer "
                + accessToken
                        .filter(token -> !token.isBlank())
                        .orElseThrow(
                                () ->
                                        new PaymentGatewayException(
                                                "Mercado Pago is not configured"));
    }

    // Erro HTTP do REST Client não pode escapar: viraria a resposta da nossa API. O 402 é a
    // exceção: a order foi criada, mas a transação falhou (cartão recusado). Isso não é falha de
    // comunicação, então vira um resultado e não um 502.
    private Charge call(Supplier<MercadoPagoOrder> request) {
        try {
            return toCharge(request.get());
        } catch (WebApplicationException e) {
            String body =
                    e.getResponse().hasEntity() ? e.getResponse().readEntity(String.class) : "";
            if (e.getResponse().getStatus() == 402) {
                return declined(body);
            }
            throw new PaymentGatewayException(
                    "Mercado Pago answered " + e.getResponse().getStatus() + ": " + body, e);
        } catch (ProcessingException e) {
            throw new PaymentGatewayException("Mercado Pago is unreachable", e);
        }
    }

    // O corpo do 402 traz os erros e, em "data", a order com a transação recusada
    // ({"errors": [...], "data": {order}}). Se não der para ler, o cartão foi recusado mesmo
    // assim, só sem o motivo.
    private Charge declined(String body) {
        try {
            JsonNode root = json.readTree(body);
            JsonNode data = root.has("data") ? root.get("data") : root;
            MercadoPagoOrder order = json.treeToValue(data, MercadoPagoOrder.class);
            if (order.id() != null) {
                Charge charge = toCharge(order);
                return charge.status() == Payment.Status.APPROVED
                        ? charge
                        : new Charge(
                                charge.externalId(),
                                Payment.Status.REJECTED,
                                charge.statusDetail(),
                                null,
                                null);
            }
        } catch (JsonProcessingException e) {
            LOG.warnf("Unreadable Mercado Pago 402 body: %s", body);
        }
        return new Charge(null, Payment.Status.REJECTED, "rejected", null, null);
    }

    private static Charge toCharge(MercadoPagoOrder order) {
        MercadoPagoOrder.Transaction transaction =
                Optional.ofNullable(order.transactions())
                        .map(MercadoPagoOrder.Transactions::payments)
                        .filter(payments -> !payments.isEmpty())
                        .map(List::getFirst)
                        .orElse(null);
        MercadoPagoOrder.PaymentMethod method =
                transaction == null ? null : transaction.paymentMethod();
        Payment.Status status = toStatus(order.status(), order.statusDetail());
        // Na recusa, o motivo útil (ex.: cc_rejected_insufficient_amount) está na transação; o
        // da order é genérico.
        String detail =
                status == Payment.Status.REJECTED
                                && transaction != null
                                && transaction.statusDetail() != null
                        ? transaction.statusDetail()
                        : order.statusDetail();
        return new Charge(
                order.id(),
                status,
                detail,
                method == null ? null : method.qrCode(),
                method == null ? null : method.qrCodeBase64());
    }

    // Tabela de docs/payments.md (Agregado Payment).
    static Payment.Status toStatus(String status, String detail) {
        boolean expired = "expired".equals(detail);
        return switch (status == null ? "" : status) {
            case "created", "processing", "action_required" -> Payment.Status.PENDING;
            // O estorno total mantém a order processed e muda só o detalhe.
            case "processed" ->
                    "refunded".equals(detail) ? Payment.Status.REFUNDED : Payment.Status.APPROVED;
            case "failed" -> expired ? Payment.Status.EXPIRED : Payment.Status.REJECTED;
            case "canceled" -> expired ? Payment.Status.EXPIRED : Payment.Status.CANCELLED;
            case "refunded" -> Payment.Status.REFUNDED;
            case "charged_back" -> Payment.Status.CHARGED_BACK;
            default -> {
                // Status novo da API: segue pendente até alguém mapear, sem aprovar nem cancelar.
                LOG.warnf("Unknown Mercado Pago order status %s/%s", status, detail);
                yield Payment.Status.PENDING;
            }
        };
    }
}

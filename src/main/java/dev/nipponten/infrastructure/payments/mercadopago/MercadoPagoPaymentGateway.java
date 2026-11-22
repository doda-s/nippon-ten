package dev.nipponten.infrastructure.payments.mercadopago;

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
                                                        "pix", "bank_transfer"),
                                                charge.expiration().toString()))),
                        new MercadoPagoOrder.Payer(charge.payerEmail(), charge.payerName()));
        return toCharge(call(() -> client.create(authorization(), charge.idempotencyKey(), body)));
    }

    @Override
    public Charge fetch(String externalId) {
        return toCharge(call(() -> client.get(authorization(), externalId)));
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

    // Erro HTTP do REST Client não pode escapar: viraria a resposta da nossa API.
    private static MercadoPagoOrder call(Supplier<MercadoPagoOrder> request) {
        try {
            return request.get();
        } catch (WebApplicationException e) {
            String body =
                    e.getResponse().hasEntity() ? e.getResponse().readEntity(String.class) : "";
            throw new PaymentGatewayException(
                    "Mercado Pago answered " + e.getResponse().getStatus() + ": " + body, e);
        } catch (ProcessingException e) {
            throw new PaymentGatewayException("Mercado Pago is unreachable", e);
        }
    }

    private static Charge toCharge(MercadoPagoOrder order) {
        MercadoPagoOrder.PaymentMethod method =
                Optional.ofNullable(order.transactions())
                        .map(MercadoPagoOrder.Transactions::payments)
                        .filter(payments -> !payments.isEmpty())
                        .map(payments -> payments.getFirst().paymentMethod())
                        .orElse(null);
        return new Charge(
                order.id(),
                toStatus(order.status(), order.statusDetail()),
                order.statusDetail(),
                method == null ? null : method.qrCode(),
                method == null ? null : method.qrCodeBase64());
    }

    // Tabela de docs/payments.md (Agregado Payment).
    static Payment.Status toStatus(String status, String detail) {
        boolean expired = "expired".equals(detail);
        return switch (status == null ? "" : status) {
            case "created", "processing", "action_required" -> Payment.Status.PENDING;
            case "processed" -> Payment.Status.APPROVED;
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

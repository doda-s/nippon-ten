package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.application.exceptions.UnauthorizedException;
import dev.nipponten.application.requests.CardPaymentRequest;
import dev.nipponten.domain.gateways.PaymentGateway;
import dev.nipponten.domain.gateways.PaymentNotificationVerifier;
import dev.nipponten.domain.models.Order;
import dev.nipponten.domain.models.Payment;
import dev.nipponten.domain.repositories.PaymentRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Pagamento online dos pedidos (docs/payments.md). A chamada ao provedor fica fora de transação,
 * para não segurar conexão de banco durante a rede; só a gravação do resultado é transacional.
 */
@ApplicationScoped
public class PaymentService {

    @Inject PaymentRepository repository;

    @Inject OrderService orderService;

    @Inject ClientService clientService;

    @Inject PaymentGateway gateway;

    @Inject PaymentNotificationVerifier verifier;

    @Inject CurrentUser currentUser;

    @Inject Clock clock;

    @ConfigProperty(name = "nippon.payments.pix-expiration")
    Duration pixExpiration;

    /**
     * Uma tentativa aberta por vez. Se a anterior ficou sem id externo (o provedor falhou ou não
     * respondeu), ela é repetida com a mesma chave de idempotência, sem cobrar duas vezes.
     */
    public Payment create(
            Long orderId, String trackingToken, Payment.Method method, CardPaymentRequest card) {
        Order order = orderService.requireCustomer(orderId, trackingToken, "pay");
        if (order.paymentChannel() != Order.PaymentChannel.ONLINE) {
            throw new InvalidRequestException("Order " + orderId + " is paid IN_PERSON");
        }
        if (method == Payment.Method.CARD && card == null) {
            throw new InvalidRequestException("card is required for CARD payments");
        }
        if (method == Payment.Method.PIX && card != null) {
            throw new InvalidRequestException("card is not accepted for PIX payments");
        }
        if (!order.paymentMethod().name().equals(method.name())) {
            throw new InvalidRequestException(
                    "Order "
                            + orderId
                            + " is paid with "
                            + order.paymentMethod()
                            + ", not "
                            + method);
        }
        if (order.paid() || order.status() != Order.Status.AWAITING_PAYMENT) {
            throw new InvalidRequestException(
                    "Order " + orderId + " is " + order.status() + " and not awaiting payment");
        }
        Optional<Payment> open =
                repository.getByOrder(orderId).stream()
                        .filter(payment -> payment.status() == Payment.Status.PENDING)
                        .findFirst();
        if (open.isPresent() && open.get().externalId() != null) {
            throw new InvalidRequestException("Order " + orderId + " already has an open payment");
        }
        Payment attempt = open.orElseGet(() -> repository.save(newAttempt(order, method, card)));
        PaymentGateway.Charge charge =
                method == Payment.Method.PIX
                        ? gateway.createPix(
                                new PaymentGateway.PixCharge(
                                        attempt.idempotencyKey(),
                                        "payment-" + attempt.id(),
                                        attempt.amount(),
                                        attempt.payerEmail(),
                                        payerName(order),
                                        pixExpiration))
                        : gateway.chargeCard(
                                new PaymentGateway.CardCharge(
                                        attempt.idempotencyKey(),
                                        "payment-" + attempt.id(),
                                        attempt.amount(),
                                        attempt.payerEmail(),
                                        payerName(order),
                                        card.identification() == null
                                                ? null
                                                : card.identification().type(),
                                        card.identification() == null
                                                ? null
                                                : card.identification().number(),
                                        card.token(),
                                        card.paymentMethodId()));
        return apply(attempt, charge);
    }

    public List<Payment> getByOrder(Long orderId, String trackingToken) {
        orderService.requireViewer(orderId, trackingToken);
        return repository.getByOrder(orderId);
    }

    /**
     * Notificação do provedor. Só a assinatura é conferida aqui; o estado vem de uma consulta ao
     * provedor, nunca do corpo da notificação, que pode chegar repetida ou fora de ordem.
     */
    public void handleNotification(
            String type, String resourceId, String requestId, String signature) {
        if (!verifier.isAuthentic(resourceId, requestId, signature)) {
            throw new UnauthorizedException("Invalid notification signature");
        }
        if (!"order".equals(type) || resourceId == null || resourceId.isBlank()) {
            return;
        }
        // Desconhecido: não é deste sistema, ou chegou antes de o id externo ser gravado (a
        // reconciliação agendada cobre esse caso).
        Payment payment = repository.getByExternalId(resourceId);
        if (payment == null) {
            return;
        }
        apply(payment, gateway.fetch(resourceId));
    }

    // Grava o que o provedor disse e, na aprovação, libera o pedido na mesma transação.
    private Payment apply(Payment payment, PaymentGateway.Charge charge) {
        if (Objects.equals(charge.externalId(), payment.externalId())
                && charge.status() == payment.status()
                && Objects.equals(charge.statusDetail(), payment.statusDetail())) {
            return payment;
        }
        return QuarkusTransaction.joiningExisting()
                .call(
                        () -> {
                            Instant now = Instant.now(clock);
                            boolean approvedNow =
                                    charge.status() == Payment.Status.APPROVED
                                            && payment.status() != Payment.Status.APPROVED;
                            Payment saved =
                                    repository.save(
                                            new Payment(
                                                    payment.id(),
                                                    payment.orderId(),
                                                    payment.provider(),
                                                    charge.externalId(),
                                                    payment.idempotencyKey(),
                                                    payment.method(),
                                                    charge.status(),
                                                    charge.statusDetail(),
                                                    payment.amount(),
                                                    payment.payerEmail(),
                                                    charge.qrCode() == null
                                                            ? payment.pixQrCode()
                                                            : charge.qrCode(),
                                                    charge.qrCodeBase64() == null
                                                            ? payment.pixQrCodeBase64()
                                                            : charge.qrCodeBase64(),
                                                    payment.expiresAt(),
                                                    approvedNow ? now : payment.approvedAt(),
                                                    payment.createdAt(),
                                                    now));
                            if (approvedNow) {
                                orderService.confirmOnlinePayment(payment.orderId(), now);
                            }
                            return saved;
                        });
    }

    // O Pix vence; o cartão não tem prazo, o resultado vem na resposta ou pelo webhook.
    private Payment newAttempt(Order order, Payment.Method method, CardPaymentRequest card) {
        Instant now = Instant.now(clock);
        String cardEmail = card == null ? null : card.payerEmail();
        return new Payment(
                null,
                order.id(),
                gateway.provider(),
                null,
                UUID.randomUUID().toString(),
                method,
                Payment.Status.PENDING,
                null,
                order.total(),
                cardEmail == null || cardEmail.isBlank() ? payerEmail(order) : cardEmail,
                null,
                null,
                method == Payment.Method.PIX ? now.plus(pixExpiration) : null,
                null,
                now,
                now);
    }

    /**
     * O provedor exige e-mail em toda cobrança. No Pix ele só serve para o comprovante: o cliente
     * usa o da conta e o visitante, o que informou no pedido. Pedido sem e-mail é o que o
     * funcionário lança, e esse nunca é online.
     */
    // Nome do cadastro do cliente ou o que o visitante informou. Só ajuda o provedor (comprovante,
    // antifraude) e nunca é obrigatório.
    private String payerName(Order order) {
        return order.clientId() == null
                ? order.guestName()
                : clientService.getById(order.clientId()).name();
    }

    private String payerEmail(Order order) {
        if (order.clientId() != null) {
            return currentUser.get().email();
        }
        if (order.guestEmail() == null) {
            throw new InvalidRequestException("Order " + order.id() + " has no payer email");
        }
        return order.guestEmail();
    }
}

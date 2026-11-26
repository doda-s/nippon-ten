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
import java.util.Optional;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Pagamento online dos pedidos (docs/payments.md). A chamada ao provedor fica fora de transação,
 * para não segurar conexão de banco durante a rede; só a gravação do resultado é transacional.
 */
@ApplicationScoped
public class PaymentService {

    @Inject PaymentRepository repository;

    @Inject OrderService orderService;

    @Inject ClientService clientService;

    @Inject PaymentSettlementService settlement;

    @Inject PaymentGateway gateway;

    @Inject PaymentNotificationVerifier verifier;

    @Inject CurrentUser currentUser;

    @Inject Clock clock;

    @ConfigProperty(name = "nippon.payments.pix-expiration")
    Duration pixExpiration;

    @ConfigProperty(name = "nippon.payments.awaiting-payment-timeout")
    Duration awaitingPaymentTimeout;

    static final String UNPAID_REASON = "Payment not completed";

    // O provedor vence o Pix junto com o nosso expires_at; a folga evita consultar antes dele.
    private static final Duration EXPIRATION_GRACE = Duration.ofMinutes(1);

    private static final int RECONCILIATION_BATCH = 100;

    // A tentativa aberta é consultada no gateway a cada rodada depois deste tempo. O webhook
    // costuma chegar antes; a consulta cobre o webhook perdido ou não configurado, para o cliente
    // não esperar o Pix vencer para ver a aprovação.
    private static final Duration PENDING_CHECK_DELAY = Duration.ofSeconds(15);

    private static final Logger LOG = Logger.getLogger(PaymentService.class);

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
            // Pix vencido ainda não reconciliado: resolve agora, para o cliente não esperar o job.
            Payment current = open.get();
            if (current.expiresAt() == null || Instant.now(clock).isBefore(current.expiresAt())) {
                throw new InvalidRequestException(
                        "Order " + orderId + " already has an open payment");
            }
            if (expire(current).status() == Payment.Status.APPROVED) {
                throw new InvalidRequestException("Order " + orderId + " is already paid");
            }
            open = Optional.empty();
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

    /**
     * Reconciliação agendada (docs/payments.md, Expiração e reconciliação). Cada item é tratado à
     * parte: uma falha do provedor num deles fica no log e não para os outros. Tudo aqui é
     * idempotente, então rodar duas vezes, ou em duas instâncias, não muda o resultado.
     */
    public void reconcile() {
        Instant now = Instant.now(clock);
        for (Payment payment :
                repository.getPendingWithoutExternalIdCreatedBefore(
                        now.minus(awaitingPaymentTimeout), RECONCILIATION_BATCH)) {
            safely(payment.orderId(), () -> giveUpUnsent(payment));
        }
        for (Payment payment :
                repository.getPendingExpiredBefore(
                        now.minus(EXPIRATION_GRACE), RECONCILIATION_BATCH)) {
            safely(payment.orderId(), () -> expire(payment));
        }
        for (Payment payment :
                repository.getPendingSentCreatedBefore(
                        now.minus(PENDING_CHECK_DELAY),
                        now.minus(EXPIRATION_GRACE),
                        RECONCILIATION_BATCH)) {
            safely(payment.orderId(), () -> apply(payment, gateway.fetch(payment.externalId())));
        }
        for (Order order :
                orderService.getAwaitingPaymentCreatedBefore(
                        now.minus(awaitingPaymentTimeout), RECONCILIATION_BATCH)) {
            safely(order.id(), () -> cancelIfAbandoned(order));
        }
        // Estorno que falhou na hora (provedor fora do ar) ou aprovação depois do cancelamento.
        for (Payment payment : repository.getApprovedOfCancelledOrders(RECONCILIATION_BATCH)) {
            safely(payment.orderId(), () -> settlement.refund(payment));
        }
    }

    // Tentativa que nunca recebeu o id do provedor, porque a criação falhou e o cliente não tentou
    // de novo. No Pix, sem o QR code ninguém consegue pagar. No cartão, a cobrança pode ter
    // acontecido sem a resposta chegar: fica o aviso para conferir no painel do provedor.
    private void giveUpUnsent(Payment payment) {
        if (payment.method() == Payment.Method.CARD) {
            LOG.warnf(
                    "Card payment %d of order %d never got an answer from the provider; check"
                            + " reference payment-%d (idempotency key %s) in the provider panel",
                    payment.id(), payment.orderId(), payment.id(), payment.idempotencyKey());
        }
        markLocally(payment, Payment.Status.EXPIRED, "not_sent");
    }

    // Pix vencido: o provedor é consultado antes, porque o webhook da aprovação pode ter se
    // perdido. Se ele ainda disser que está aberto, o Pix é vencido aqui mesmo.
    private Payment expire(Payment payment) {
        if (payment.externalId() == null) {
            return markLocally(payment, Payment.Status.EXPIRED, "not_sent");
        }
        Payment updated = apply(payment, gateway.fetch(payment.externalId()));
        return updated.status() == Payment.Status.PENDING
                ? markLocally(updated, Payment.Status.EXPIRED, "expired")
                : updated;
    }

    // Enquanto houver tentativa aberta (Pix no prazo, cartão em análise), o pedido espera. A
    // consulta fica numa transação nova: fora dela, a leitura usa a sessão do request, que ainda
    // guarda os pagamentos como estavam antes de a etapa anterior vencê-los.
    private void cancelIfAbandoned(Order order) {
        QuarkusTransaction.requiringNew()
                .run(
                        () -> {
                            boolean open =
                                    repository.getByOrder(order.id()).stream()
                                            .anyMatch(
                                                    payment ->
                                                            payment.status()
                                                                    == Payment.Status.PENDING);
                            if (!open) {
                                orderService.cancelUnpaid(order.id(), UNPAID_REASON);
                            }
                        });
    }

    private Payment markLocally(Payment payment, Payment.Status status, String detail) {
        return apply(
                payment,
                new PaymentGateway.Charge(payment.externalId(), status, detail, null, null));
    }

    private static void safely(Long orderId, Runnable step) {
        try {
            step.run();
        } catch (RuntimeException e) {
            LOG.warnf(e, "Payment reconciliation failed for order %d", orderId);
        }
    }

    /**
     * Grava o que o provedor disse e, na aprovação, libera o pedido na mesma transação. Uma
     * aprovação que chega depois de o pedido ser cancelado é estornada em seguida; se o estorno
     * falhar, a reconciliação agendada tenta de novo.
     */
    private Payment apply(Payment payment, PaymentGateway.Charge charge) {
        if (!PaymentUpdates.changes(payment, charge)) {
            return payment;
        }
        boolean approvedNow =
                charge.status() == Payment.Status.APPROVED
                        && payment.status() != Payment.Status.APPROVED;
        Payment saved =
                QuarkusTransaction.joiningExisting()
                        .call(
                                () -> {
                                    Instant now = Instant.now(clock);
                                    Payment updated =
                                            repository.save(
                                                    PaymentUpdates.withCharge(
                                                            payment, charge, now));
                                    if (approvedNow) {
                                        orderService.confirmOnlinePayment(payment.orderId(), now);
                                    }
                                    if (updated.status() == Payment.Status.CHARGED_BACK
                                            && payment.status() != Payment.Status.CHARGED_BACK) {
                                        orderService.notifyChargeback(payment.orderId());
                                    }
                                    return updated;
                                });
        if (approvedNow && isCancelled(saved.orderId())) {
            try {
                return settlement.refund(saved);
            } catch (RuntimeException e) {
                LOG.warnf(e, "Refund of payment %d of a cancelled order failed", saved.id());
            }
        }
        return saved;
    }

    // Numa transação nova, para não ler o pedido da sessão do request, que pode estar velha.
    private boolean isCancelled(Long orderId) {
        return QuarkusTransaction.requiringNew()
                .call(() -> orderService.getById(orderId).status() == Order.Status.CANCELLED);
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

package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.PaymentGatewayException;
import dev.nipponten.application.exceptions.PaymentRefundRejectedException;
import dev.nipponten.domain.gateways.PaymentGateway;
import dev.nipponten.domain.models.Order;
import dev.nipponten.domain.models.Payment;
import dev.nipponten.domain.repositories.PaymentRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.jboss.logging.Logger;

/**
 * Desfaz no provedor os pagamentos de um pedido que vai ser cancelado (docs/payments.md,
 * Cancelamento e estorno): cancela a tentativa aberta e estorna a aprovada. Fica separado do
 * PaymentService porque o OrderService precisa dele, e o PaymentService já depende do OrderService.
 * Depende só do repositório e do gateway, sem ciclo.
 *
 * <p>As chamadas ao provedor ficam fora de transação; cada resultado é gravado na transação do
 * repositório. A chave de idempotência do cancelamento e do estorno deriva da tentativa, então
 * repetir a operação (o cliente clica duas vezes, o job tenta de novo) não estorna duas vezes.
 */
@ApplicationScoped
public class PaymentSettlementService {

    private static final Logger LOG = Logger.getLogger(PaymentSettlementService.class);

    // O Mercado Pago recusa (422) o estorno de um cartão aprovado há poucos segundos e aceita
    // minutos depois. Recusa que passa disso não se resolve sozinha: no sandbox, por exemplo, a
    // order do Pix fica paga sem o dinheiro ter entrado, e o estorno é recusado para sempre.
    static final Duration REFUND_REJECTION_WINDOW = Duration.ofHours(1);

    @Inject PaymentRepository repository;

    @Inject PaymentGateway gateway;

    @Inject Clock clock;

    /**
     * Antes de cancelar o pedido. Uma falha do provedor não impede o cancelamento: o Mercado Pago
     * recusa por alguns segundos o estorno de um cartão recém-aprovado, e o cliente não deve ficar
     * preso nisso. O que falhou fica para a reconciliação agendada: o pagamento aprovado de pedido
     * cancelado é estornado a cada rodada, e a tentativa aberta é resolvida quando vence.
     */
    public void settle(Order order) {
        for (Payment payment : repository.getByOrder(order.id())) {
            try {
                switch (payment.status()) {
                    case PENDING -> cancelAttempt(payment);
                    case APPROVED -> refund(payment);
                    default -> {}
                }
            } catch (PaymentGatewayException e) {
                LOG.warnf(
                        e,
                        "Could not settle payment %d of order %d now; the reconciliation retries it",
                        payment.id(),
                        order.id());
            }
        }
    }

    /**
     * Estorno total. Também usado para o pagamento aprovado depois de o pedido ser cancelado.
     *
     * <p>Estorno recusado pelo provedor volta a ser tentado só até {@link #REFUND_REJECTION_WINDOW}
     * depois da aprovação. Depois disso o pagamento fica marcado {@link Payment#REFUND_REJECTED}, a
     * reconciliação para de insistir e o log pede o estorno manual.
     */
    public Payment refund(Payment payment) {
        try {
            return save(
                    payment,
                    gateway.refund(payment.externalId(), payment.idempotencyKey() + "-refund"));
        } catch (PaymentRefundRejectedException e) {
            Instant approvedAt =
                    payment.approvedAt() != null ? payment.approvedAt() : payment.updatedAt();
            if (Instant.now(clock).isBefore(approvedAt.plus(REFUND_REJECTION_WINDOW))) {
                throw e;
            }
            LOG.errorf(
                    "The provider keeps rejecting the refund of payment %d of order %d (%s);"
                            + " stopped retrying, refund provider order %s by hand",
                    payment.id(), payment.orderId(), e.getMessage(), payment.externalId());
            return save(
                    payment,
                    new PaymentGateway.Charge(
                            payment.externalId(),
                            payment.status(),
                            Payment.REFUND_REJECTED,
                            null,
                            null));
        }
    }

    private void cancelAttempt(Payment payment) {
        if (payment.externalId() == null) {
            if (payment.method() == Payment.Method.CARD) {
                LOG.warnf(
                        "Cancelling card payment %d of order %d that never got an answer from the"
                                + " provider; check reference payment-%d in the provider panel",
                        payment.id(), payment.orderId(), payment.id());
            }
            save(
                    payment,
                    new PaymentGateway.Charge(
                            null, Payment.Status.CANCELLED, "not_sent", null, null));
            return;
        }
        Payment saved =
                save(
                        payment,
                        gateway.cancel(payment.externalId(), payment.idempotencyKey() + "-cancel"));
        // Pago no último instante: não dá mais para cancelar, então estorna. Um cartão em análise
        // continua PENDING; se for aprovado depois, o PaymentService estorna na aprovação.
        if (saved.status() == Payment.Status.APPROVED) {
            refund(saved);
        }
    }

    private Payment save(Payment payment, PaymentGateway.Charge charge) {
        if (!PaymentUpdates.changes(payment, charge)) {
            return payment;
        }
        return repository.save(PaymentUpdates.withCharge(payment, charge, Instant.now(clock)));
    }
}

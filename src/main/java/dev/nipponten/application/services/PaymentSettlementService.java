package dev.nipponten.application.services;

import dev.nipponten.domain.gateways.PaymentGateway;
import dev.nipponten.domain.models.Order;
import dev.nipponten.domain.models.Payment;
import dev.nipponten.domain.repositories.PaymentRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
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

    @Inject PaymentRepository repository;

    @Inject PaymentGateway gateway;

    @Inject Clock clock;

    /** Antes de cancelar o pedido. Uma falha do provedor impede o cancelamento (502). */
    public void settle(Order order) {
        for (Payment payment : repository.getByOrder(order.id())) {
            switch (payment.status()) {
                case PENDING -> cancelAttempt(payment);
                case APPROVED -> refund(payment);
                default -> {}
            }
        }
    }

    /** Estorno total. Também usado para o pagamento aprovado depois de o pedido ser cancelado. */
    public Payment refund(Payment payment) {
        return save(
                payment,
                gateway.refund(payment.externalId(), payment.idempotencyKey() + "-refund"));
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

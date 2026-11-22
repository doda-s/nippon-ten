package dev.nipponten.infrastructure.scheduling;

import dev.nipponten.application.services.PaymentService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Dispara a reconciliação dos pagamentos online (docs/payments.md, Expiração e reconciliação). Em
 * teste o scheduler fica desligado e os testes chamam o PaymentService direto, com o TestClock.
 */
@ApplicationScoped
public class PaymentReconciliationJob {

    @Inject PaymentService paymentService;

    // SKIP: uma rodada lenta (provedor fora do ar) não empilha outra por cima.
    @Scheduled(
            identity = "payment-reconciliation",
            every = "{nippon.payments.reconciliation-interval}",
            delayed = "{nippon.payments.reconciliation-interval}",
            concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void run() {
        paymentService.reconcile();
    }
}

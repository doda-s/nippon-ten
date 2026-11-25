package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.UnauthorizedException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Jobs disparados de fora por HTTP (docs/deploy.md, Jobs agendados). No Cloud Run a CPU só fica
 * alocada durante uma request, então o scheduler interno não roda com o serviço ocioso: lá quem
 * chama é o Cloud Scheduler, autenticado por um token compartilhado. Sem token configurado, nenhuma
 * chamada é aceita.
 */
@ApplicationScoped
public class JobTriggerService {

    @Inject PaymentService paymentService;

    @ConfigProperty(name = "nippon.jobs.token")
    Optional<String> token;

    // Uma rodada lenta (provedor fora do ar) não empilha outra por cima, como o SKIP do
    // PaymentReconciliationJob. A reconciliação é idempotente; isto só evita trabalho repetido.
    private final AtomicBoolean reconciling = new AtomicBoolean();

    public void reconcilePayments(String presentedToken) {
        requireToken(presentedToken);
        if (!reconciling.compareAndSet(false, true)) {
            return;
        }
        try {
            paymentService.reconcile();
        } finally {
            reconciling.set(false);
        }
    }

    private void requireToken(String presentedToken) {
        if (token.isEmpty()
                || presentedToken == null
                || !MessageDigest.isEqual(
                        token.get().getBytes(StandardCharsets.UTF_8),
                        presentedToken.getBytes(StandardCharsets.UTF_8))) {
            throw new UnauthorizedException("Invalid job token");
        }
    }
}

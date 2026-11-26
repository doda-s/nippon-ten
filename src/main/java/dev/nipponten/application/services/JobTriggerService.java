package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.UnauthorizedException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Jobs disparados de fora por HTTP (docs/deploy.md, Jobs agendados). No Cloud Run a CPU só fica
 * alocada durante uma request, então o scheduler interno não roda com o serviço ocioso: lá quem
 * chama é o Cloud Scheduler, autenticado por um token compartilhado. Sem token configurado, nenhuma
 * chamada é aceita.
 */
@ApplicationScoped
public class JobTriggerService {

    private static final Logger LOG = Logger.getLogger(JobTriggerService.class);

    @Inject PaymentService paymentService;

    @Inject GeocodingService geocodingService;

    @ConfigProperty(name = "nippon.jobs.token")
    Optional<String> token;

    // Uma rodada lenta (provedor fora do ar) não empilha outra por cima, como o SKIP dos jobs do
    // scheduler interno. Os dois jobs são idempotentes; isto só evita trabalho repetido.
    private final AtomicBoolean reconciling = new AtomicBoolean();
    private final AtomicBoolean purgingGeocodeCache = new AtomicBoolean();

    /** Mesma rodada do PaymentReconciliationJob. */
    public void reconcilePayments(String presentedToken) {
        requireToken(presentedToken);
        runAlone(reconciling, paymentService::reconcile);
    }

    /** Mesma limpeza do GeocodeCachePurgeJob (docs/delivery.md, cache de 29 dias). */
    public void purgeGeocodeCache(String presentedToken) {
        requireToken(presentedToken);
        runAlone(
                purgingGeocodeCache,
                () -> {
                    int removed = geocodingService.purgeExpired();
                    if (removed > 0) {
                        LOG.infof("Removed %d expired geocoded addresses", removed);
                    }
                });
    }

    private static void runAlone(AtomicBoolean running, Runnable job) {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            job.run();
        } finally {
            running.set(false);
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

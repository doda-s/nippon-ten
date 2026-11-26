package dev.nipponten.infrastructure.scheduling;

import dev.nipponten.application.services.GeocodingService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Apaga as coordenadas do Google com mais de nippon.delivery.geocode-cache-ttl, como os termos do
 * Google exigem (no máximo 30 dias). Em teste o scheduler fica desligado.
 */
@ApplicationScoped
public class GeocodeCachePurgeJob {

    private static final Logger LOG = Logger.getLogger(GeocodeCachePurgeJob.class);

    @Inject GeocodingService geocodingService;

    @Scheduled(
            identity = "geocode-cache-purge",
            cron = "{nippon.delivery.geocode-cache-purge-cron}",
            concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void run() {
        int removed = geocodingService.purgeExpired();
        if (removed > 0) {
            LOG.infof("Removed %d expired geocoded addresses", removed);
        }
    }
}

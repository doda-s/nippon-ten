package dev.nipponten.application.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.nipponten.application.exceptions.TooManyRequestsException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Limita os pedidos de reenvio do link de acompanhamento, em janelas fixas que começam no primeiro
 * pedido. Conta por código (para ninguém encher a caixa do visitante) e por IP (contra testar
 * combinações de código e e-mail). Todo pedido conta, acerte ou não, para o limite não revelar
 * quais combinações existem.
 *
 * <p>O estado fica em memória, como no LoginAttemptLimiter: com mais de uma instância da API, cada
 * uma conta separado.
 */
@ApplicationScoped
public class TrackingLinkLimiter {

    static final String MESSAGE = "Too many tracking link requests, try again later";

    private record Requests(int count, Instant windowEnd) {
        boolean isOpenAt(Instant now) {
            return now.isBefore(windowEnd);
        }
    }

    @ConfigProperty(name = "nippon.orders.tracking-link.max-per-order")
    int maxPerOrder;

    @ConfigProperty(name = "nippon.orders.tracking-link.max-per-ip")
    int maxPerIp;

    @ConfigProperty(name = "nippon.orders.tracking-link.window")
    Duration window;

    private Cache<String, Requests> byCode;

    private Cache<String, Requests> byIp;

    @Inject Clock clock;

    @PostConstruct
    void init() {
        byCode = newCache();
        byIp = newCache();
    }

    private Cache<String, Requests> newCache() {
        // O expire só libera memória; quem decide se a janela acabou é o windowEnd.
        return Caffeine.newBuilder().expireAfterWrite(window).maximumSize(100_000).build();
    }

    /** Confere o limite e já conta o pedido. */
    public void acquire(String code, String ip) {
        Instant now = Instant.now(clock);
        requireUnderLimit(byCode.getIfPresent(normalize(code)), maxPerOrder, now);
        if (ip != null) {
            requireUnderLimit(byIp.getIfPresent(ip), maxPerIp, now);
        }
        increment(byCode, normalize(code), now);
        if (ip != null) {
            increment(byIp, ip, now);
        }
    }

    /** Zera todos os contadores. Usado nos testes, que compartilham o IP 127.0.0.1. */
    public void reset() {
        byCode.invalidateAll();
        byIp.invalidateAll();
    }

    private void increment(Cache<String, Requests> cache, String key, Instant now) {
        cache.asMap()
                .compute(
                        key,
                        (k, current) ->
                                current == null || !current.isOpenAt(now)
                                        ? new Requests(1, now.plus(window))
                                        : new Requests(current.count() + 1, current.windowEnd()));
    }

    private static void requireUnderLimit(Requests requests, int max, Instant now) {
        if (requests != null && requests.isOpenAt(now) && requests.count() >= max) {
            throw new TooManyRequestsException(
                    MESSAGE, Duration.between(now, requests.windowEnd()));
        }
    }

    private static String normalize(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}

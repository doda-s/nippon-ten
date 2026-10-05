package dev.nipponten.application.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.nipponten.application.exceptions.TooManyRequestsException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Limita logins que falham, em janelas fixas que começam na primeira falha. Conta por email (contra
 * força bruta numa conta) e por IP (contra testar uma senha em muitas contas). O email é contado
 * mesmo quando não existe, para o limite não revelar quais estão cadastrados. Login certo zera o
 * contador do email, mas não o do IP.
 *
 * <p>O estado fica em memória: com mais de uma instância da API, cada uma conta separado.
 */
@ApplicationScoped
public class LoginAttemptLimiter {

    static final String MESSAGE = "Too many login attempts, try again later";

    private record Attempts(int failures, Instant windowEnd) {
        boolean isOpenAt(Instant now) {
            return now.isBefore(windowEnd);
        }
    }

    @ConfigProperty(name = "nippon.auth.login.max-failures-per-email")
    int maxFailuresPerEmail;

    @ConfigProperty(name = "nippon.auth.login.max-failures-per-ip")
    int maxFailuresPerIp;

    @ConfigProperty(name = "nippon.auth.login.window")
    Duration window;

    private Cache<String, Attempts> byEmail;

    private Cache<String, Attempts> byIp;

    @PostConstruct
    void init() {
        byEmail = newCache();
        byIp = newCache();
    }

    private Cache<String, Attempts> newCache() {
        // O expire só libera memória; quem decide se a janela acabou é o windowEnd.
        return Caffeine.newBuilder().expireAfterWrite(window).maximumSize(100_000).build();
    }

    /** Antes de conferir a senha, para quem estourou o limite nem chegar ao bcrypt. */
    public void check(String email, String ip) {
        Instant now = Instant.now();
        requireUnderLimit(byEmail.getIfPresent(normalize(email)), maxFailuresPerEmail, now);
        if (ip != null) {
            requireUnderLimit(byIp.getIfPresent(ip), maxFailuresPerIp, now);
        }
    }

    public void recordFailure(String email, String ip) {
        Instant now = Instant.now();
        increment(byEmail, normalize(email), now);
        if (ip != null) {
            increment(byIp, ip, now);
        }
    }

    public void recordSuccess(String email) {
        byEmail.invalidate(normalize(email));
    }

    /** Zera todos os contadores. Usado nos testes, que compartilham o IP 127.0.0.1. */
    public void reset() {
        byEmail.invalidateAll();
        byIp.invalidateAll();
    }

    private void increment(Cache<String, Attempts> cache, String key, Instant now) {
        cache.asMap()
                .compute(
                        key,
                        (k, current) ->
                                current == null || !current.isOpenAt(now)
                                        ? new Attempts(1, now.plus(window))
                                        : new Attempts(
                                                current.failures() + 1, current.windowEnd()));
    }

    private static void requireUnderLimit(Attempts attempts, int max, Instant now) {
        if (attempts != null && attempts.isOpenAt(now) && attempts.failures() >= max) {
            throw new TooManyRequestsException(
                    MESSAGE, Duration.between(now, attempts.windowEnd()));
        }
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

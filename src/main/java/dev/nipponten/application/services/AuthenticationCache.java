package dev.nipponten.application.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.nipponten.domain.models.AuthenticatedUser;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Cache curto do resultado da checagem de sessão e permissões, por sessão. Toda mudança que afeta
 * acesso (logout, revogação, desativação, permissões, roles) invalida as entradas na hora, então
 * nesta instância a revogação continua imediata; o TTL só limita o atraso em outras instâncias da
 * API, que não recebem a invalidação.
 */
@ApplicationScoped
public class AuthenticationCache {

    private record Key(Long userId, UUID sessionId) {}

    @ConfigProperty(name = "nippon.auth.session-cache-ttl")
    Duration ttl;

    @ConfigProperty(name = "nippon.auth.session-cache-max-size")
    long maxSize;

    @Inject TransactionSynchronizationRegistry transactions;

    private Cache<Key, Optional<AuthenticatedUser>> cache;

    @PostConstruct
    void init() {
        cache = Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(maxSize).build();
    }

    // Guarda também o resultado vazio (sessão encerrada), para um token revogado não virar uma
    // consulta ao banco a cada request.
    Optional<AuthenticatedUser> get(
            Long userId, UUID sessionId, Supplier<Optional<AuthenticatedUser>> loader) {
        return cache.get(new Key(userId, sessionId), key -> loader.get());
    }

    public void invalidateSession(UUID sessionId) {
        invalidate(key -> key.sessionId().equals(sessionId));
    }

    public void invalidateUser(Long userId) {
        invalidate(key -> key.userId().equals(userId));
    }

    /** Para mudanças em roles, que atingem um número desconhecido de usuários. */
    public void invalidateAll() {
        afterCommitToo(cache::invalidateAll);
    }

    private void invalidate(Predicate<Key> matches) {
        afterCommitToo(() -> cache.asMap().keySet().removeIf(matches));
    }

    // Invalidar só antes do commit deixaria uma brecha: um request concorrente leria o banco ainda
    // sem a mudança e guardaria o valor antigo. Dentro de transação, invalida de novo ao terminar.
    private void afterCommitToo(Runnable invalidation) {
        invalidation.run();
        if (transactions.getTransactionStatus() == Status.STATUS_ACTIVE) {
            transactions.registerInterposedSynchronization(
                    new Synchronization() {
                        @Override
                        public void beforeCompletion() {}

                        @Override
                        public void afterCompletion(int status) {
                            invalidation.run();
                        }
                    });
        }
    }
}

package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.UnauthorizedException;
import dev.nipponten.domain.models.RefreshToken;
import dev.nipponten.domain.repositories.RefreshTokenRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Refresh tokens opacos, guardados só como SHA-256. Todo uso gera um token novo e revoga o atual;
 * apresentar um token já revogado indica roubo, e aí todos os tokens do usuário são revogados.
 */
@ApplicationScoped
public class RefreshTokenService {

    private static final String INVALID_REFRESH_TOKEN = "Invalid refresh token";

    @Inject RefreshTokenRepository repository;

    @Inject AuthenticationCache authenticationCache;

    @Inject Clock clock;

    @ConfigProperty(name = "nippon.auth.refresh-token-ttl")
    Duration ttl;

    public record IssuedRefreshToken(Long userId, UUID sessionId, String value, Duration ttl) {}

    /** Login: abre uma sessão nova. */
    public IssuedRefreshToken issue(Long userId) {
        return issue(userId, UUID.randomUUID(), Instant.now(clock)).issued();
    }

    // dontRollbackOn: a revogação em massa por reuso precisa ficar gravada mesmo com o 401.
    @Transactional(dontRollbackOn = UnauthorizedException.class)
    public IssuedRefreshToken rotate(String value) {
        Instant now = Instant.now(clock);
        RefreshToken current = requireKnown(value);
        if (current.isRevoked()) {
            revokeAllByUser(current.userId());
            throw new UnauthorizedException(INVALID_REFRESH_TOKEN);
        }
        if (current.isExpiredAt(now)) {
            throw new UnauthorizedException(INVALID_REFRESH_TOKEN);
        }
        Issued next = issue(current.userId(), current.sessionId(), now);
        repository.save(
                new RefreshToken(
                        current.id(),
                        current.userId(),
                        current.sessionId(),
                        current.tokenHash(),
                        current.expiresAt(),
                        current.createdAt(),
                        now,
                        next.model().id()));
        return next.issued();
    }

    /** Logout: revoga o token informado, se ele existir e ainda estiver ativo. */
    public void revoke(String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        RefreshToken current = repository.getByHash(OpaqueTokens.hash(value));
        if (current != null && !current.isRevoked()) {
            repository.save(
                    new RefreshToken(
                            current.id(),
                            current.userId(),
                            current.sessionId(),
                            current.tokenHash(),
                            current.expiresAt(),
                            current.createdAt(),
                            Instant.now(clock),
                            current.replacedById()));
            authenticationCache.invalidateSession(current.sessionId());
        }
    }

    public void revokeAllByUser(Long userId) {
        repository.revokeAllByUser(userId, Instant.now(clock));
        authenticationCache.invalidateUser(userId);
    }

    public void removeAllByUser(Long userId) {
        repository.removeAllByUser(userId);
        authenticationCache.invalidateUser(userId);
    }

    private record Issued(RefreshToken model, IssuedRefreshToken issued) {}

    private Issued issue(Long userId, UUID sessionId, Instant now) {
        String value = OpaqueTokens.generate();
        RefreshToken saved =
                repository.save(
                        new RefreshToken(
                                null,
                                userId,
                                sessionId,
                                OpaqueTokens.hash(value),
                                now.plus(ttl),
                                now,
                                null,
                                null));
        return new Issued(saved, new IssuedRefreshToken(userId, sessionId, value, ttl));
    }

    private RefreshToken requireKnown(String value) {
        if (value == null || value.isBlank()) {
            throw new UnauthorizedException(INVALID_REFRESH_TOKEN);
        }
        RefreshToken token = repository.getByHash(OpaqueTokens.hash(value));
        if (token == null) {
            throw new UnauthorizedException(INVALID_REFRESH_TOKEN);
        }
        return token;
    }
}

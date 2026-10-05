package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.UnauthorizedException;
import dev.nipponten.domain.models.RefreshToken;
import dev.nipponten.domain.repositories.RefreshTokenRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Refresh tokens opacos, guardados só como SHA-256. Todo uso gera um token novo e revoga o atual;
 * apresentar um token já revogado indica roubo, e aí todos os tokens do usuário são revogados.
 */
@ApplicationScoped
public class RefreshTokenService {

    private static final String INVALID_REFRESH_TOKEN = "Invalid refresh token";

    private static final SecureRandom RANDOM = new SecureRandom();

    @Inject RefreshTokenRepository repository;

    @ConfigProperty(name = "nippon.auth.refresh-token-ttl")
    Duration ttl;

    public record IssuedRefreshToken(Long userId, UUID sessionId, String value, Duration ttl) {}

    /** Login: abre uma sessão nova. */
    public IssuedRefreshToken issue(Long userId) {
        return issue(userId, UUID.randomUUID(), LocalDateTime.now()).issued();
    }

    /** O access token só vale enquanto a sessão de onde ele saiu estiver ativa. */
    public boolean isSessionActive(UUID sessionId) {
        return repository.isSessionActive(sessionId, LocalDateTime.now());
    }

    // dontRollbackOn: a revogação em massa por reuso precisa ficar gravada mesmo com o 401.
    @Transactional(dontRollbackOn = UnauthorizedException.class)
    public IssuedRefreshToken rotate(String value) {
        LocalDateTime now = LocalDateTime.now();
        RefreshToken current = requireKnown(value);
        if (current.isRevoked()) {
            repository.revokeAllByUser(current.userId(), now);
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
        RefreshToken current = repository.getByHash(hash(value));
        if (current != null && !current.isRevoked()) {
            repository.save(
                    new RefreshToken(
                            current.id(),
                            current.userId(),
                            current.sessionId(),
                            current.tokenHash(),
                            current.expiresAt(),
                            current.createdAt(),
                            LocalDateTime.now(),
                            current.replacedById()));
        }
    }

    public void revokeAllByUser(Long userId) {
        repository.revokeAllByUser(userId, LocalDateTime.now());
    }

    public void removeAllByUser(Long userId) {
        repository.removeAllByUser(userId);
    }

    private record Issued(RefreshToken model, IssuedRefreshToken issued) {}

    private Issued issue(Long userId, UUID sessionId, LocalDateTime now) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken saved =
                repository.save(
                        new RefreshToken(
                                null,
                                userId,
                                sessionId,
                                hash(value),
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
        RefreshToken token = repository.getByHash(hash(value));
        if (token == null) {
            throw new UnauthorizedException(INVALID_REFRESH_TOKEN);
        }
        return token;
    }

    // O token é aleatório com 256 bits, então SHA-256 basta; bcrypt seria custo sem ganho.
    private static String hash(String value) {
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.RefreshToken;
import java.time.LocalDateTime;
import java.util.UUID;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken model);

    RefreshToken getByHash(String tokenHash);

    /** Revoga, com a data informada, todos os tokens ainda não revogados do usuário. */
    void revokeAllByUser(Long userId, LocalDateTime revokedAt);

    void removeAllByUser(Long userId);

    /** Uma sessão está ativa enquanto tiver um refresh token não revogado e não expirado. */
    boolean isSessionActive(UUID sessionId, LocalDateTime now);
}

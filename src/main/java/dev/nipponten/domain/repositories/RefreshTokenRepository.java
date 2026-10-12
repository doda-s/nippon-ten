package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.RefreshToken;
import java.time.Instant;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken model);

    RefreshToken getByHash(String tokenHash);

    /** Revoga, com a data informada, todos os tokens ainda não revogados do usuário. */
    void revokeAllByUser(Long userId, Instant revokedAt);

    void removeAllByUser(Long userId);
}

package dev.nipponten.domain.models;

import java.time.Instant;
import java.util.UUID;

public record RefreshToken(
        Long id,
        Long userId,
        UUID sessionId,
        String tokenHash,
        Instant expiresAt,
        Instant createdAt,
        Instant revokedAt,
        Long replacedById) {

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpiredAt(Instant now) {
        return !expiresAt.isAfter(now);
    }
}

package dev.nipponten.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public record RefreshToken(
        Long id,
        Long userId,
        UUID sessionId,
        String tokenHash,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime revokedAt,
        Long replacedById) {

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpiredAt(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }
}

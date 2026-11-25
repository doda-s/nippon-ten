package dev.nipponten.domain.models;

import java.time.Instant;

/**
 * Linha do extrato. points tem sinal. Linha positiva com remaining é um lote que ainda pode ser
 * gasto até expiresAt; gastos, estornos e expirações apontam o lote afetado em lotId.
 */
public record PointsTransaction(
        Long id,
        Long clientId,
        Long orderId,
        Type type,
        int points,
        Integer remaining,
        Instant expiresAt,
        Long lotId,
        String reason,
        Long createdByUserId,
        Instant createdAt) {

    public enum Type {
        EARN,
        REDEEM,
        REFUND,
        EXPIRE,
        ADJUST
    }
}

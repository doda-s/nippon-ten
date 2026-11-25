package dev.nipponten.application.responses;

import dev.nipponten.domain.models.PointsTransaction;
import java.time.Instant;

// Linha do extrato; o controle interno de lotes (remaining, lotId) não sai da API.
public record PointsTransactionResponse(
        Long id,
        PointsTransaction.Type type,
        int points,
        Long orderId,
        Instant expiresAt,
        String reason,
        Instant createdAt) {

    public static PointsTransactionResponse of(PointsTransaction transaction) {
        return new PointsTransactionResponse(
                transaction.id(),
                transaction.type(),
                transaction.points(),
                transaction.orderId(),
                transaction.expiresAt(),
                transaction.reason(),
                transaction.createdAt());
    }
}

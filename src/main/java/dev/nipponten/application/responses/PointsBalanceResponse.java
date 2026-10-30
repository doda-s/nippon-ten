package dev.nipponten.application.responses;

import dev.nipponten.domain.models.PointsBalance;
import java.time.Instant;

// nextExpiration é nulo quando nenhum ponto do saldo vence.
public record PointsBalanceResponse(int balance, NextExpiration nextExpiration) {

    public record NextExpiration(int points, Instant expiresAt) {}

    public static PointsBalanceResponse of(PointsBalance balance) {
        return new PointsBalanceResponse(
                balance.balance(),
                balance.nextExpiring() == null
                        ? null
                        : new NextExpiration(
                                balance.nextExpiring().remaining(),
                                balance.nextExpiring().expiresAt()));
    }
}

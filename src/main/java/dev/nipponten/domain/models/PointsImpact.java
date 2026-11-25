package dev.nipponten.domain.models;

import java.math.BigDecimal;

/**
 * Quanto um cliente ganharia comprando amount, com a configuração atual e com a proposta (req. 37).
 */
public record PointsImpact(BigDecimal amount, Scenario current, Scenario proposed) {

    /**
     * pointsEarned = pontos que a compra renderia; pointsWorth = quanto esses pontos valem em
     * desconto.
     */
    public record Scenario(
            BigDecimal earnRate, BigDecimal pointValue, int pointsEarned, BigDecimal pointsWorth) {}
}

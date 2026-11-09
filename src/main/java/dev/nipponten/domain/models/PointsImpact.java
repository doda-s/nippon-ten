package dev.nipponten.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Impacto de uma configuração de pontos sobre os pedidos concluídos de clientes num período,
 * comparado com a configuração atual (req. 37). Custo = pontos × valor do ponto. amount e points
 * são a compra e os pontos de exemplo da simulação, nulos quando não informados.
 */
public record PointsImpact(
        LocalDate from,
        LocalDate to,
        long orders,
        BigDecimal revenue,
        Scenario current,
        Scenario proposed,
        long pointsRedeemed,
        long outstandingPoints,
        BigDecimal amount,
        Integer points) {

    /**
     * Pontos que a taxa geraria no período, quanto eles custariam ao serem resgatados, a fração da
     * receita que isso representa e o custo do saldo em aberto hoje. pointsForAmount = pontos que a
     * compra de exemplo renderia; pointsWorth = quanto os pontos de exemplo valem em desconto.
     */
    public record Scenario(
            BigDecimal earnRate,
            BigDecimal pointValue,
            long pointsEarned,
            BigDecimal cost,
            BigDecimal costShareOfRevenue,
            BigDecimal outstandingLiability,
            Integer pointsForAmount,
            BigDecimal pointsWorth) {}
}

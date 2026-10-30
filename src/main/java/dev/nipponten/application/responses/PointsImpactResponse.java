package dev.nipponten.application.responses;

import dev.nipponten.domain.models.PointsImpact;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PointsImpactResponse(
        LocalDate from,
        LocalDate to,
        long orders,
        BigDecimal revenue,
        Scenario current,
        Scenario proposed,
        long pointsRedeemed,
        long outstandingPoints) {

    public record Scenario(
            BigDecimal earnRate,
            BigDecimal pointValue,
            long pointsEarned,
            BigDecimal cost,
            BigDecimal costShareOfRevenue,
            BigDecimal outstandingLiability) {

        static Scenario of(PointsImpact.Scenario scenario) {
            return new Scenario(
                    scenario.earnRate(),
                    scenario.pointValue(),
                    scenario.pointsEarned(),
                    scenario.cost(),
                    scenario.costShareOfRevenue(),
                    scenario.outstandingLiability());
        }
    }

    public static PointsImpactResponse of(PointsImpact impact) {
        return new PointsImpactResponse(
                impact.from(),
                impact.to(),
                impact.orders(),
                impact.revenue(),
                Scenario.of(impact.current()),
                Scenario.of(impact.proposed()),
                impact.pointsRedeemed(),
                impact.outstandingPoints());
    }
}

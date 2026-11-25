package dev.nipponten.application.responses;

import dev.nipponten.domain.models.PointsImpact;
import java.math.BigDecimal;

public record PointsImpactResponse(BigDecimal amount, Scenario current, Scenario proposed) {

    public record Scenario(
            BigDecimal earnRate, BigDecimal pointValue, int pointsEarned, BigDecimal pointsWorth) {

        static Scenario of(PointsImpact.Scenario scenario) {
            return new Scenario(
                    scenario.earnRate(),
                    scenario.pointValue(),
                    scenario.pointsEarned(),
                    scenario.pointsWorth());
        }
    }

    public static PointsImpactResponse of(PointsImpact impact) {
        return new PointsImpactResponse(
                impact.amount(), Scenario.of(impact.current()), Scenario.of(impact.proposed()));
    }
}

package dev.nipponten.application.responses;

import dev.nipponten.domain.models.PointsSettings;
import java.math.BigDecimal;
import java.time.Instant;

public record PointsSettingsResponse(
        BigDecimal earnRate, BigDecimal pointValue, Integer expirationDays, Instant updatedAt) {

    public static PointsSettingsResponse of(PointsSettings settings) {
        return new PointsSettingsResponse(
                settings.earnRate(),
                settings.pointValue(),
                settings.expirationDays(),
                settings.updatedAt());
    }
}

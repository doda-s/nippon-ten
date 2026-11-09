package dev.nipponten.application.requests;

import dev.nipponten.domain.models.PointsSettings;
import jakarta.enterprise.context.ApplicationScoped;

// Quem alterou e quando são preenchidos pelo service.
@ApplicationScoped
public class PointsSettingsRequestMapper {

    public PointsSettings toModel(PointsSettingsRequest request) {
        return new PointsSettings(
                request.earnRate(), request.pointValue(), request.expirationDays(), null, null);
    }
}

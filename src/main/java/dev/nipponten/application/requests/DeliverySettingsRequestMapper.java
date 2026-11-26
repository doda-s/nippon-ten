package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Coordinates;
import dev.nipponten.domain.models.DeliverySettings;
import jakarta.enterprise.context.ApplicationScoped;

// Quem alterou e quando são preenchidos pelo service.
@ApplicationScoped
public class DeliverySettingsRequestMapper {

    public DeliverySettings toModel(DeliverySettingsRequest request) {
        return new DeliverySettings(
                new Coordinates(request.storeLatitude(), request.storeLongitude()),
                request.tiers().stream()
                        .map(tier -> new DeliverySettings.Tier(tier.maxDistanceKm(), tier.fee()))
                        .toList(),
                null,
                null);
    }
}

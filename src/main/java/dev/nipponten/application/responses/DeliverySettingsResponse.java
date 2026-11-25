package dev.nipponten.application.responses;

import dev.nipponten.domain.models.DeliverySettings;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * enabled = false enquanto a loja não foi configurada: a taxa é 0. maxDistanceKm é o raio de
 * entrega, a maior faixa.
 */
public record DeliverySettingsResponse(
        boolean enabled,
        Double storeLatitude,
        Double storeLongitude,
        List<Tier> tiers,
        BigDecimal maxDistanceKm,
        Instant updatedAt) {

    public record Tier(BigDecimal maxDistanceKm, BigDecimal fee) {}

    public static DeliverySettingsResponse of(DeliverySettings settings) {
        boolean enabled = settings.storeLocation() != null;
        return new DeliverySettingsResponse(
                enabled,
                enabled ? settings.storeLocation().latitude() : null,
                enabled ? settings.storeLocation().longitude() : null,
                settings.tiers().stream()
                        .map(tier -> new Tier(tier.maxDistanceKm(), tier.fee()))
                        .toList(),
                settings.tiers().isEmpty() ? null : settings.tiers().getLast().maxDistanceKm(),
                settings.updatedAt());
    }
}

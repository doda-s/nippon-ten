package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Coordinates;
import dev.nipponten.domain.models.DeliverySettings;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * enabled = false enquanto a loja não foi configurada: a taxa é 0. storeLocation é de onde a
 * distância é medida: o pino, se storeLocationPinned, ou a coordenada do endereço, que sai nula se
 * o geocoder estiver fora do ar. storeAddress é nulo na configuração feita só com coordenadas,
 * antes do endereço existir. maxDistanceKm é o raio de entrega, a maior faixa.
 */
public record DeliverySettingsResponse(
        boolean enabled,
        StoreAddressResponse storeAddress,
        StoreLocationResponse storeLocation,
        boolean storeLocationPinned,
        List<Tier> tiers,
        BigDecimal maxDistanceKm,
        Instant updatedAt) {

    public record Tier(BigDecimal maxDistanceKm, BigDecimal fee) {}

    public static DeliverySettingsResponse of(DeliverySettings settings, Coordinates location) {
        return new DeliverySettingsResponse(
                settings.enabled(),
                StoreAddressResponse.of(settings.storeAddress()),
                StoreLocationResponse.of(location),
                settings.pinnedLocation() != null,
                settings.tiers().stream()
                        .map(tier -> new Tier(tier.maxDistanceKm(), tier.fee()))
                        .toList(),
                settings.tiers().isEmpty() ? null : settings.tiers().getLast().maxDistanceKm(),
                settings.updatedAt());
    }
}

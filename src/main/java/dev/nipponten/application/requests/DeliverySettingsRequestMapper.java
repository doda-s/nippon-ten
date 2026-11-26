package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Address;
import dev.nipponten.domain.models.Coordinates;
import dev.nipponten.domain.models.DeliverySettings;
import jakarta.enterprise.context.ApplicationScoped;

// Bairro, cidade e UF da loja, quem alterou e quando são preenchidos pelo service.
@ApplicationScoped
public class DeliverySettingsRequestMapper {

    public DeliverySettings toModel(DeliverySettingsRequest request) {
        DeliverySettingsRequest.PinnedLocation pin = request.pinnedLocation();
        return new DeliverySettings(
                toAddress(request.storeAddress()),
                pin == null ? null : new Coordinates(pin.latitude(), pin.longitude()),
                request.tiers().stream()
                        .map(tier -> new DeliverySettings.Tier(tier.maxDistanceKm(), tier.fee()))
                        .toList(),
                null,
                null);
    }

    public Address toAddress(StoreAddressRequest request) {
        return new Address(
                request.streetAddress(),
                request.number(),
                request.complement(),
                request.cep(),
                null,
                null,
                null);
    }
}

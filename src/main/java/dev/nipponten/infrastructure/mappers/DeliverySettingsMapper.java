package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.Address;
import dev.nipponten.domain.models.Coordinates;
import dev.nipponten.domain.models.DeliverySettings;
import dev.nipponten.infrastructure.entities.DeliveryFeeTierEmbeddable;
import dev.nipponten.infrastructure.entities.DeliverySettingsEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DeliverySettingsMapper {

    public void updateEntity(DeliverySettingsEntity entity, DeliverySettings model) {
        Address store = model.storeAddress();
        entity.setStoreStreetAddress(store == null ? null : store.streetAddress());
        entity.setStoreNumber(store == null ? null : store.number());
        entity.setStoreComplement(store == null ? null : store.complement());
        entity.setStoreCep(store == null ? null : store.cep());
        entity.setStoreNeighborhood(store == null ? null : store.neighborhood());
        entity.setStoreCity(store == null ? null : store.city());
        entity.setStoreState(store == null ? null : store.state());
        Coordinates pin = model.pinnedLocation();
        entity.setStoreLatitude(pin == null ? null : pin.latitude());
        entity.setStoreLongitude(pin == null ? null : pin.longitude());
        entity.getTiers().clear();
        model.tiers()
                .forEach(
                        tier ->
                                entity.getTiers()
                                        .add(
                                                new DeliveryFeeTierEmbeddable(
                                                        tier.maxDistanceKm(), tier.fee())));
        entity.setUpdatedAt(model.updatedAt());
        entity.setUpdatedByUserId(model.updatedByUserId());
    }

    public DeliverySettings toModel(DeliverySettingsEntity entity) {
        Address store =
                entity.getStoreCep() == null
                        ? null
                        : new Address(
                                entity.getStoreStreetAddress(),
                                entity.getStoreNumber(),
                                entity.getStoreComplement(),
                                entity.getStoreCep(),
                                entity.getStoreNeighborhood(),
                                entity.getStoreCity(),
                                entity.getStoreState());
        Coordinates pin =
                entity.getStoreLatitude() == null || entity.getStoreLongitude() == null
                        ? null
                        : new Coordinates(entity.getStoreLatitude(), entity.getStoreLongitude());
        return new DeliverySettings(
                store,
                pin,
                entity.getTiers().stream()
                        .map(
                                tier ->
                                        new DeliverySettings.Tier(
                                                tier.getMaxDistanceKm(), tier.getFee()))
                        .toList(),
                entity.getUpdatedAt(),
                entity.getUpdatedByUserId());
    }
}

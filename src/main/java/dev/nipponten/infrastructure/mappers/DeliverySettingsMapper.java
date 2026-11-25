package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.Coordinates;
import dev.nipponten.domain.models.DeliverySettings;
import dev.nipponten.infrastructure.entities.DeliveryFeeTierEmbeddable;
import dev.nipponten.infrastructure.entities.DeliverySettingsEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DeliverySettingsMapper {

    public void updateEntity(DeliverySettingsEntity entity, DeliverySettings model) {
        Coordinates store = model.storeLocation();
        entity.setStoreLatitude(store == null ? null : store.latitude());
        entity.setStoreLongitude(store == null ? null : store.longitude());
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
        Coordinates store =
                entity.getStoreLatitude() == null || entity.getStoreLongitude() == null
                        ? null
                        : new Coordinates(entity.getStoreLatitude(), entity.getStoreLongitude());
        return new DeliverySettings(
                store,
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

package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.PointsSettings;
import dev.nipponten.infrastructure.entities.PointsSettingsEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PointsSettingsMapper {

    public void updateEntity(PointsSettingsEntity entity, PointsSettings model) {
        entity.setEarnRate(model.earnRate());
        entity.setPointValue(model.pointValue());
        entity.setExpirationDays(model.expirationDays());
        entity.setUpdatedAt(model.updatedAt());
        entity.setUpdatedByUserId(model.updatedByUserId());
    }

    public PointsSettings toModel(PointsSettingsEntity entity) {
        return new PointsSettings(
                entity.getEarnRate(),
                entity.getPointValue(),
                entity.getExpirationDays(),
                entity.getUpdatedAt(),
                entity.getUpdatedByUserId());
    }
}

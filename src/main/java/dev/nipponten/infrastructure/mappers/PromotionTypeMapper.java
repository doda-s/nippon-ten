package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.PromotionType;
import dev.nipponten.infrastructure.entities.PromotionTypeEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PromotionTypeMapper {

    public PromotionTypeEntity toEntity(PromotionType model) {
        PromotionTypeEntity entity = new PromotionTypeEntity();
        applyToEntity(entity, model);
        return entity;
    }

    public void updateEntity(PromotionTypeEntity entity, PromotionType model) {
        applyToEntity(entity, model);
    }

    private void applyToEntity(PromotionTypeEntity entity, PromotionType model) {
        entity.setName(model.name());
        entity.setDescription(model.description());
        entity.setType(model.type());
        entity.setValue(model.value());
    }

    public PromotionType toModel(PromotionTypeEntity entity) {
        return new PromotionType(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getType(),
                entity.getValue());
    }
}

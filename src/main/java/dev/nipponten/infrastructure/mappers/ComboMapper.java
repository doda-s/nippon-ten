package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.Combo;
import dev.nipponten.infrastructure.entities.ComboEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ComboMapper {

    public ComboEntity toEntity(Combo model) {
        ComboEntity entity = new ComboEntity();
        applyToEntity(entity, model);
        return entity;
    }

    public void updateEntity(ComboEntity entity, Combo model) {
        applyToEntity(entity, model);
    }

    private void applyToEntity(ComboEntity entity, Combo model) {
        entity.setName(model.name());
        entity.setPrice(model.price());
        entity.setImageUrl(model.imageUrl());
        entity.setDescription(model.description());
        entity.setStatus(ComboEntity.Status.valueOf(model.status().name()));
    }

    public Combo toModel(ComboEntity entity) {
        return new Combo(
                entity.getId(),
                entity.getName(),
                entity.getPrice(),
                entity.getImageUrl(),
                entity.getDescription(),
                Combo.Status.valueOf(entity.getStatus().name()));
    }
}

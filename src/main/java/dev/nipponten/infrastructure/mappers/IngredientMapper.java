package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.Ingredient;
import dev.nipponten.infrastructure.entities.IngredientEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IngredientMapper {

    public IngredientEntity toEntity(Ingredient model) {
        IngredientEntity entity = new IngredientEntity();
        applyToEntity(entity, model);
        return entity;
    }

    public void updateEntity(IngredientEntity entity, Ingredient model) {
        applyToEntity(entity, model);
    }

    private void applyToEntity(IngredientEntity entity, Ingredient model) {
        entity.setName(model.name());
        entity.setDescription(model.description());
        entity.setImageUrl(model.imageUrl());
        entity.setPrice(model.price());
        entity.setStatus(IngredientEntity.Status.valueOf(model.status().name()));
    }

    public Ingredient toModel(IngredientEntity entity) {
        return new Ingredient(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getImageUrl(),
                entity.getPrice(),
                Ingredient.Status.valueOf(entity.getStatus().name()));
    }
}

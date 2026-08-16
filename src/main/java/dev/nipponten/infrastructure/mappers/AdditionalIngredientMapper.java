package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.AdditionalIngredient;
import dev.nipponten.infrastructure.entities.AdditionalIngredientEntity;
import dev.nipponten.infrastructure.entities.IngredientEntity;
import dev.nipponten.infrastructure.entities.ProductEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class AdditionalIngredientMapper {

    @Inject
    EntityManager entityManager;

    public AdditionalIngredientEntity toEntity(AdditionalIngredient model) {
        AdditionalIngredientEntity entity = new AdditionalIngredientEntity();
        applyToEntity(entity, model);
        return entity;
    }

    public void updateEntity(AdditionalIngredientEntity entity, AdditionalIngredient model) {
        applyToEntity(entity, model);
    }

    private void applyToEntity(AdditionalIngredientEntity entity, AdditionalIngredient model) {
        entity.setProduct(entityManager.getReference(ProductEntity.class, model.productId()));
        entity.setIngredient(entityManager.getReference(IngredientEntity.class, model.ingredientId()));
        entity.setMaximumQuantity(model.maximumQuantity());
        entity.setStatus(AdditionalIngredientEntity.Status.valueOf(model.status().name()));
    }

    public AdditionalIngredient toModel(AdditionalIngredientEntity entity) {
        return new AdditionalIngredient(
                entity.getId(),
                entity.getProduct().getId(),
                entity.getIngredient().getId(),
                entity.getMaximumQuantity(),
                AdditionalIngredient.Status.valueOf(entity.getStatus().name()));
    }
}

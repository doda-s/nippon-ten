package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.ProductIngredient;
import dev.nipponten.infrastructure.entities.IngredientEntity;
import dev.nipponten.infrastructure.entities.ProductEntity;
import dev.nipponten.infrastructure.entities.ProductIngredientEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class ProductIngredientMapper {

    @Inject EntityManager entityManager;

    public ProductIngredientEntity toEntity(ProductIngredient model) {
        ProductIngredientEntity entity = new ProductIngredientEntity();
        applyToEntity(entity, model);
        return entity;
    }

    public void updateEntity(ProductIngredientEntity entity, ProductIngredient model) {
        applyToEntity(entity, model);
    }

    private void applyToEntity(ProductIngredientEntity entity, ProductIngredient model) {
        entity.setProduct(entityManager.getReference(ProductEntity.class, model.productId()));
        entity.setIngredient(
                entityManager.getReference(IngredientEntity.class, model.ingredientId()));
    }

    public ProductIngredient toModel(ProductIngredientEntity entity) {
        return new ProductIngredient(
                entity.getId(), entity.getProduct().getId(), entity.getIngredient().getId());
    }
}

package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.ProductSize;
import dev.nipponten.infrastructure.entities.ProductEntity;
import dev.nipponten.infrastructure.entities.ProductSizeEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class ProductSizeMapper {

    @Inject EntityManager entityManager;

    public ProductSizeEntity toEntity(ProductSize model) {
        ProductSizeEntity entity = new ProductSizeEntity();
        applyToEntity(entity, model);
        return entity;
    }

    public void updateEntity(ProductSizeEntity entity, ProductSize model) {
        applyToEntity(entity, model);
    }

    private void applyToEntity(ProductSizeEntity entity, ProductSize model) {
        entity.setProduct(entityManager.getReference(ProductEntity.class, model.productId()));
        entity.setPrice(model.price());
        entity.setStatus(ProductSizeEntity.Status.valueOf(model.status().name()));
    }

    public ProductSize toModel(ProductSizeEntity entity) {
        return new ProductSize(
                entity.getId(),
                entity.getProduct().getId(),
                entity.getPrice(),
                ProductSize.Status.valueOf(entity.getStatus().name()));
    }
}

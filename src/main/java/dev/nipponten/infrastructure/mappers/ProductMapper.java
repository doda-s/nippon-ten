package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.Product;
import dev.nipponten.infrastructure.entities.ProductEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProductMapper {

    public ProductEntity toEntity(Product model) {
        ProductEntity entity = new ProductEntity();
        applyToEntity(entity, model);
        return entity;
    }

    public void updateEntity(ProductEntity entity, Product model) {
        applyToEntity(entity, model);
    }

    private void applyToEntity(ProductEntity entity, Product model) {
        entity.setName(model.name());
        entity.setImageUrl(model.imageUrl());
        entity.setDescription(model.description());
        entity.setStatus(ProductEntity.Status.valueOf(model.status().name()));
    }

    public Product toModel(ProductEntity entity) {
        return new Product(
                entity.getId(),
                entity.getName(),
                entity.getImageUrl(),
                entity.getDescription(),
                Product.Status.valueOf(entity.getStatus().name()));
    }
}

package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.ComboProduct;
import dev.nipponten.infrastructure.entities.ComboEntity;
import dev.nipponten.infrastructure.entities.ComboProductEntity;
import dev.nipponten.infrastructure.entities.ProductEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class ComboProductMapper {

    @Inject
    EntityManager entityManager;

    public ComboProductEntity toEntity(ComboProduct model) {
        ComboProductEntity entity = new ComboProductEntity();
        applyToEntity(entity, model);
        return entity;
    }

    public void updateEntity(ComboProductEntity entity, ComboProduct model) {
        applyToEntity(entity, model);
    }

    private void applyToEntity(ComboProductEntity entity, ComboProduct model) {
        entity.setCombo(entityManager.getReference(ComboEntity.class, model.comboId()));
        entity.setProduct(entityManager.getReference(ProductEntity.class, model.productId()));
    }

    public ComboProduct toModel(ComboProductEntity entity) {
        return new ComboProduct(
                entity.getId(),
                entity.getCombo().getId(),
                entity.getProduct().getId());
    }
}

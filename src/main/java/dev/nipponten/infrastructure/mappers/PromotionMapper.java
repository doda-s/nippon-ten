package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.Promotion;
import dev.nipponten.infrastructure.entities.ProductEntity;
import dev.nipponten.infrastructure.entities.PromotionEntity;
import dev.nipponten.infrastructure.entities.PromotionTypeEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class PromotionMapper {

    @Inject
    EntityManager entityManager;

    public PromotionEntity toEntity(Promotion model) {
        PromotionEntity entity = new PromotionEntity();
        applyToEntity(entity, model);
        return entity;
    }

    public void updateEntity(PromotionEntity entity, Promotion model) {
        applyToEntity(entity, model);
    }

    private void applyToEntity(PromotionEntity entity, Promotion model) {
        entity.setTitle(model.title());
        entity.setPrice(model.price());
        entity.setImageUrl(model.imageUrl());
        entity.setDescription(model.description());
        entity.setStatus(PromotionEntity.Status.valueOf(model.status().name()));
        entity.setPromotionType(entityManager.getReference(PromotionTypeEntity.class, model.promotionTypeId()));
        entity.setProduct(entityManager.getReference(ProductEntity.class, model.productId()));
        entity.setStartDate(model.startDate());
        entity.setEndDate(model.endDate());
        entity.setEnablePromotionPoints(model.enablePromotionPoints());
    }

    public Promotion toModel(PromotionEntity entity) {
        return new Promotion(
                entity.getId(),
                entity.getTitle(),
                entity.getPrice(),
                entity.getImageUrl(),
                entity.getDescription(),
                Promotion.Status.valueOf(entity.getStatus().name()),
                entity.getPromotionType().getId(),
                entity.getProduct().getId(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.isEnablePromotionPoints());
    }
}

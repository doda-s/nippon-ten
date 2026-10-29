package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.Promotion;
import dev.nipponten.infrastructure.entities.ComboEntity;
import dev.nipponten.infrastructure.entities.ProductEntity;
import dev.nipponten.infrastructure.entities.PromotionEntity;
import dev.nipponten.infrastructure.entities.PromotionTypeEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class PromotionMapper {

    @Inject EntityManager entityManager;

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
        entity.setImageUrl(model.imageUrl());
        entity.setDescription(model.description());
        entity.setStatus(PromotionEntity.Status.valueOf(model.status().name()));
        entity.setPromotionType(
                entityManager.getReference(PromotionTypeEntity.class, model.promotionTypeId()));
        // As listas chegam sempre completas: substituem as anteriores.
        entity.getProducts().clear();
        model.productIds()
                .forEach(
                        id ->
                                entity.getProducts()
                                        .add(entityManager.getReference(ProductEntity.class, id)));
        entity.getCombos().clear();
        model.comboIds()
                .forEach(
                        id ->
                                entity.getCombos()
                                        .add(entityManager.getReference(ComboEntity.class, id)));
        entity.setStartDate(model.startDate());
        entity.setEndDate(model.endDate());
        entity.setEnablePromotionPoints(model.enablePromotionPoints());
    }

    public Promotion toModel(PromotionEntity entity) {
        return new Promotion(
                entity.getId(),
                entity.getTitle(),
                entity.getImageUrl(),
                entity.getDescription(),
                Promotion.Status.valueOf(entity.getStatus().name()),
                entity.getPromotionType().getId(),
                entity.getProducts().stream().map(ProductEntity::getId).sorted().toList(),
                entity.getCombos().stream().map(ComboEntity::getId).sorted().toList(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.isEnablePromotionPoints());
    }
}

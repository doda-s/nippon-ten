package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.PointsTransaction;
import dev.nipponten.infrastructure.entities.PointsTransactionEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PointsTransactionMapper {

    public PointsTransactionEntity toEntity(PointsTransaction model) {
        PointsTransactionEntity entity = new PointsTransactionEntity();
        applyToEntity(entity, model);
        return entity;
    }

    public void updateEntity(PointsTransactionEntity entity, PointsTransaction model) {
        applyToEntity(entity, model);
    }

    private void applyToEntity(PointsTransactionEntity entity, PointsTransaction model) {
        entity.setClientId(model.clientId());
        entity.setOrderId(model.orderId());
        entity.setType(PointsTransactionEntity.Type.valueOf(model.type().name()));
        entity.setPoints(model.points());
        entity.setRemaining(model.remaining());
        entity.setExpiresAt(model.expiresAt());
        entity.setLotId(model.lotId());
        entity.setReason(model.reason());
        entity.setCreatedByUserId(model.createdByUserId());
        entity.setCreatedAt(model.createdAt());
    }

    public PointsTransaction toModel(PointsTransactionEntity entity) {
        return new PointsTransaction(
                entity.getId(),
                entity.getClientId(),
                entity.getOrderId(),
                PointsTransaction.Type.valueOf(entity.getType().name()),
                entity.getPoints(),
                entity.getRemaining(),
                entity.getExpiresAt(),
                entity.getLotId(),
                entity.getReason(),
                entity.getCreatedByUserId(),
                entity.getCreatedAt());
    }
}

package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.Order;
import dev.nipponten.domain.models.OrderStatusChange;
import dev.nipponten.infrastructure.entities.OrderEntity;
import dev.nipponten.infrastructure.entities.OrderStatusHistoryEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderStatusHistoryMapper {

    public OrderStatusHistoryEntity toEntity(OrderStatusChange model) {
        OrderStatusHistoryEntity entity = new OrderStatusHistoryEntity();
        entity.setOrderId(model.orderId());
        entity.setStatus(OrderEntity.Status.valueOf(model.status().name()));
        entity.setChangedAt(model.changedAt());
        entity.setChangedByUserId(model.changedByUserId());
        return entity;
    }

    public OrderStatusChange toModel(OrderStatusHistoryEntity entity) {
        return new OrderStatusChange(
                entity.getId(),
                entity.getOrderId(),
                Order.Status.valueOf(entity.getStatus().name()),
                entity.getChangedAt(),
                entity.getChangedByUserId());
    }
}

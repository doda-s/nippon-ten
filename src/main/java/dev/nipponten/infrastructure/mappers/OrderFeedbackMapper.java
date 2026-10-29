package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.OrderFeedback;
import dev.nipponten.infrastructure.entities.OrderFeedbackEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderFeedbackMapper {

    public OrderFeedbackEntity toEntity(OrderFeedback model) {
        OrderFeedbackEntity entity = new OrderFeedbackEntity();
        entity.setOrderId(model.orderId());
        entity.setRating(model.rating());
        entity.setComment(model.comment());
        entity.setCreatedAt(model.createdAt());
        return entity;
    }

    public OrderFeedback toModel(OrderFeedbackEntity entity) {
        return new OrderFeedback(
                entity.getId(),
                entity.getOrderId(),
                entity.getRating(),
                entity.getComment(),
                entity.getCreatedAt());
    }
}

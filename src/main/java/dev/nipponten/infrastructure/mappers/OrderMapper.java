package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.Order;
import dev.nipponten.domain.models.OrderItem;
import dev.nipponten.domain.models.OrderItemAdditional;
import dev.nipponten.domain.models.OrderItemComboComponent;
import dev.nipponten.domain.models.OrderItemRemovedIngredient;
import dev.nipponten.infrastructure.entities.OrderEntity;
import dev.nipponten.infrastructure.entities.OrderItemAdditionalEntity;
import dev.nipponten.infrastructure.entities.OrderItemComboComponentEntity;
import dev.nipponten.infrastructure.entities.OrderItemEntity;
import dev.nipponten.infrastructure.entities.OrderItemRemovedIngredientEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderMapper {

    public OrderEntity toEntity(Order model) {
        OrderEntity entity = new OrderEntity();
        updateEntity(entity, model);
        return entity;
    }

    // Os itens são substituídos inteiros: um item editado é um item novo, com o preço do momento.
    public void updateEntity(OrderEntity entity, Order model) {
        entity.setCode(model.code());
        entity.setClientId(model.clientId());
        entity.setCreatedByInternalId(model.createdByInternalId());
        entity.setTrackingTokenHash(model.trackingTokenHash());
        entity.setGuestName(model.guestName());
        entity.setGuestPhone(model.guestPhone());
        entity.setFulfillmentType(
                OrderEntity.FulfillmentType.valueOf(model.fulfillmentType().name()));
        entity.setStatus(OrderEntity.Status.valueOf(model.status().name()));
        entity.setPaymentMethod(OrderEntity.PaymentMethod.valueOf(model.paymentMethod().name()));
        entity.setPaid(model.paid());
        entity.setPaidAt(model.paidAt());
        entity.setStreetAddress(model.streetAddress());
        entity.setNumber(model.number());
        entity.setCep(model.cep());
        entity.setComplement(model.complement());
        entity.setNotes(model.notes());
        entity.setCancelReason(model.cancelReason());
        entity.setSubtotal(model.subtotal());
        entity.setDiscount(model.discount());
        entity.setDeliveryFee(model.deliveryFee());
        entity.setTotal(model.total());
        entity.setCreatedAt(model.createdAt());
        entity.setUpdatedAt(model.updatedAt());
        // Item sem id veio de uma criação ou edição; itens já gravados não mudam.
        if (model.items().stream().anyMatch(item -> item.id() == null)) {
            entity.getItems().clear();
            model.items().forEach(item -> entity.getItems().add(toEntity(entity, item)));
        }
    }

    private OrderItemEntity toEntity(OrderEntity order, OrderItem model) {
        OrderItemEntity entity = new OrderItemEntity();
        entity.setOrder(order);
        entity.setType(OrderItemEntity.Type.valueOf(model.type().name()));
        entity.setProductSizeId(model.productSizeId());
        entity.setComboId(model.comboId());
        entity.setPromotionId(model.promotionId());
        entity.setName(model.name());
        entity.setSizeName(model.sizeName());
        entity.setQuantity(model.quantity());
        entity.setOriginalUnitPrice(model.originalUnitPrice());
        entity.setUnitPrice(model.unitPrice());
        entity.setTotal(model.total());
        entity.setNotes(model.notes());
        for (OrderItemAdditional additional : model.additionals()) {
            OrderItemAdditionalEntity child = new OrderItemAdditionalEntity();
            child.setOrderItem(entity);
            child.setIngredientId(additional.ingredientId());
            child.setName(additional.name());
            child.setQuantity(additional.quantity());
            child.setUnitPrice(additional.unitPrice());
            entity.getAdditionals().add(child);
        }
        for (OrderItemRemovedIngredient removed : model.removedIngredients()) {
            OrderItemRemovedIngredientEntity child = new OrderItemRemovedIngredientEntity();
            child.setOrderItem(entity);
            child.setIngredientId(removed.ingredientId());
            child.setName(removed.name());
            entity.getRemovedIngredients().add(child);
        }
        for (OrderItemComboComponent component : model.comboComponents()) {
            OrderItemComboComponentEntity child = new OrderItemComboComponentEntity();
            child.setOrderItem(entity);
            child.setProductId(component.productId());
            child.setProductSizeId(component.productSizeId());
            child.setName(component.name());
            child.setSizeName(component.sizeName());
            child.setQuantity(component.quantity());
            entity.getComboComponents().add(child);
        }
        return entity;
    }

    public Order toModel(OrderEntity entity) {
        return new Order(
                entity.getId(),
                entity.getCode(),
                entity.getClientId(),
                entity.getCreatedByInternalId(),
                entity.getTrackingTokenHash(),
                entity.getGuestName(),
                entity.getGuestPhone(),
                Order.FulfillmentType.valueOf(entity.getFulfillmentType().name()),
                Order.Status.valueOf(entity.getStatus().name()),
                Order.PaymentMethod.valueOf(entity.getPaymentMethod().name()),
                entity.isPaid(),
                entity.getPaidAt(),
                entity.getStreetAddress(),
                entity.getNumber(),
                entity.getCep(),
                entity.getComplement(),
                entity.getNotes(),
                entity.getCancelReason(),
                entity.getSubtotal(),
                entity.getDiscount(),
                entity.getDeliveryFee(),
                entity.getTotal(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getItems().stream().map(this::toModel).toList());
    }

    private OrderItem toModel(OrderItemEntity entity) {
        return new OrderItem(
                entity.getId(),
                entity.getOrder().getId(),
                OrderItem.Type.valueOf(entity.getType().name()),
                entity.getProductSizeId(),
                entity.getComboId(),
                entity.getPromotionId(),
                entity.getName(),
                entity.getSizeName(),
                entity.getQuantity(),
                entity.getOriginalUnitPrice(),
                entity.getUnitPrice(),
                entity.getTotal(),
                entity.getNotes(),
                entity.getAdditionals().stream()
                        .map(
                                child ->
                                        new OrderItemAdditional(
                                                child.getId(),
                                                child.getIngredientId(),
                                                child.getName(),
                                                child.getQuantity(),
                                                child.getUnitPrice()))
                        .toList(),
                entity.getRemovedIngredients().stream()
                        .map(
                                child ->
                                        new OrderItemRemovedIngredient(
                                                child.getId(),
                                                child.getIngredientId(),
                                                child.getName()))
                        .toList(),
                entity.getComboComponents().stream()
                        .map(
                                child ->
                                        new OrderItemComboComponent(
                                                child.getId(),
                                                child.getProductId(),
                                                child.getProductSizeId(),
                                                child.getName(),
                                                child.getSizeName(),
                                                child.getQuantity()))
                        .toList());
    }
}

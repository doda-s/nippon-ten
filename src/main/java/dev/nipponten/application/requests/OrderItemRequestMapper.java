package dev.nipponten.application.requests;

import dev.nipponten.domain.models.OrderItem;
import dev.nipponten.domain.models.OrderItemAdditional;
import dev.nipponten.domain.models.OrderItemRemovedIngredient;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

// Só ids, quantidades e observação vêm do cliente; nome e preço são preenchidos pelo service.
@ApplicationScoped
public class OrderItemRequestMapper {

    public OrderItem toModel(OrderItemRequest request) {
        List<OrderItemAdditional> additionals =
                request.additionals() == null
                        ? List.of()
                        : request.additionals().stream()
                                .map(
                                        additional ->
                                                new OrderItemAdditional(
                                                        null,
                                                        additional.ingredientId(),
                                                        null,
                                                        additional.quantity(),
                                                        null))
                                .toList();
        List<OrderItemRemovedIngredient> removed =
                request.removedIngredientIds() == null
                        ? List.of()
                        : request.removedIngredientIds().stream()
                                .map(id -> new OrderItemRemovedIngredient(null, id, null))
                                .toList();
        return new OrderItem(
                null,
                null,
                request.type(),
                request.productSizeId(),
                request.comboId(),
                null,
                null,
                null,
                request.quantity(),
                null,
                null,
                null,
                request.notes(),
                additionals,
                removed,
                List.of());
    }

    public List<OrderItem> toModels(List<OrderItemRequest> requests) {
        return requests.stream().map(this::toModel).toList();
    }
}

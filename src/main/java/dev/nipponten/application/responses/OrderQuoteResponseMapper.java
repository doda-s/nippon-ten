package dev.nipponten.application.responses;

import dev.nipponten.domain.models.OrderItem;
import dev.nipponten.domain.models.OrderQuote;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderQuoteResponseMapper {

    public OrderQuoteResponse toResponse(OrderQuote quote) {
        return new OrderQuoteResponse(
                quote.items().stream().map(this::toResponse).toList(),
                quote.subtotal(),
                quote.discount(),
                quote.deliveryFee(),
                quote.total());
    }

    public OrderItemResponse toResponse(OrderItem item) {
        return new OrderItemResponse(
                item.type(),
                item.productSizeId(),
                item.comboId(),
                item.promotionId(),
                item.name(),
                item.sizeName(),
                item.quantity(),
                item.originalUnitPrice(),
                item.unitPrice(),
                item.total(),
                item.notes(),
                item.additionals().stream()
                        .map(
                                additional ->
                                        new OrderItemResponse.Additional(
                                                additional.ingredientId(),
                                                additional.name(),
                                                additional.quantity(),
                                                additional.unitPrice()))
                        .toList(),
                item.removedIngredients().stream()
                        .map(
                                removed ->
                                        new OrderItemResponse.RemovedIngredient(
                                                removed.ingredientId(), removed.name()))
                        .toList(),
                item.comboComponents().stream()
                        .map(
                                component ->
                                        new OrderItemResponse.ComboComponent(
                                                component.productId(),
                                                component.productSizeId(),
                                                component.name(),
                                                component.sizeName(),
                                                component.quantity()))
                        .toList());
    }
}

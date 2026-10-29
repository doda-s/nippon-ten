package dev.nipponten.application.responses;

import dev.nipponten.domain.models.OrderItem;
import java.math.BigDecimal;
import java.util.List;

public record OrderItemResponse(
        OrderItem.Type type,
        Long productSizeId,
        Long comboId,
        Long promotionId,
        String name,
        String sizeName,
        int quantity,
        BigDecimal originalUnitPrice,
        BigDecimal unitPrice,
        BigDecimal total,
        String notes,
        List<Additional> additionals,
        List<RemovedIngredient> removedIngredients,
        List<ComboComponent> comboComponents) {

    public record Additional(Long ingredientId, String name, int quantity, BigDecimal unitPrice) {}

    public record RemovedIngredient(Long ingredientId, String name) {}

    public record ComboComponent(
            Long productId, Long productSizeId, String name, String sizeName, int quantity) {}
}

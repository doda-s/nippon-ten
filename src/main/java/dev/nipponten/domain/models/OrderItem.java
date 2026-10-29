package dev.nipponten.domain.models;

import java.math.BigDecimal;
import java.util.List;

// Nome, tamanho e preços são cópias do catálogo no momento do pedido: o histórico não pode mudar
// quando o produto muda de preço ou é excluído.
public record OrderItem(
        Long id,
        Long orderId,
        Type type,
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
        List<OrderItemAdditional> additionals,
        List<OrderItemRemovedIngredient> removedIngredients,
        List<OrderItemComboComponent> comboComponents) {

    public enum Type {
        PRODUCT,
        COMBO
    }
}

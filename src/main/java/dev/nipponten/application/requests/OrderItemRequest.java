package dev.nipponten.application.requests;

import dev.nipponten.domain.models.OrderItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

// Quais ids cada tipo exige (productSizeId para PRODUCT, comboId para COMBO) depende do tipo, por
// isso é conferido no service. Nenhum preço entra pelo request.
public record OrderItemRequest(
        @NotNull OrderItem.Type type,
        Long productSizeId,
        Long comboId,
        @NotNull @Positive Integer quantity,
        @Size(max = 500) String notes,
        List<@NotNull @Valid OrderItemAdditionalRequest> additionals,
        List<@NotNull Long> removedIngredientIds) {}

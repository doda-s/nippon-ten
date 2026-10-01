package dev.nipponten.application.requests;

import dev.nipponten.domain.models.AdditionalIngredient;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AdditionalIngredientRequestMapper {

    public AdditionalIngredient toModel(
            Long id, Long productId, AdditionalIngredientRequest request) {
        return new AdditionalIngredient(
                id, productId, request.ingredientId(), request.maximumQuantity(), request.status());
    }
}

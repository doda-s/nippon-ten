package dev.nipponten.application.responses;

import dev.nipponten.domain.models.AdditionalIngredient;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AdditionalIngredientResponseMapper {

    public AdditionalIngredientResponse toResponse(AdditionalIngredient additionalIngredient) {
        return new AdditionalIngredientResponse(
                additionalIngredient.id(),
                additionalIngredient.productId(),
                additionalIngredient.ingredientId(),
                additionalIngredient.maximumQuantity(),
                additionalIngredient.status());
    }
}

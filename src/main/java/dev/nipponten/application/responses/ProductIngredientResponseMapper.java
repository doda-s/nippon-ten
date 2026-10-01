package dev.nipponten.application.responses;

import dev.nipponten.domain.models.ProductIngredient;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProductIngredientResponseMapper {

    public ProductIngredientResponse toResponse(ProductIngredient productIngredient) {
        return new ProductIngredientResponse(
                productIngredient.id(),
                productIngredient.productId(),
                productIngredient.ingredientId());
    }
}

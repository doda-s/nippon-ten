package dev.nipponten.application.requests;

import dev.nipponten.domain.models.ProductIngredient;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProductIngredientRequestMapper {

    public ProductIngredient toModel(Long id, Long productId, ProductIngredientRequest request) {
        return new ProductIngredient(id, productId, request.ingredientId());
    }
}

package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Ingredient;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IngredientResponseMapper {

    public IngredientResponse toResponse(Ingredient ingredient) {
        return new IngredientResponse(
                ingredient.id(),
                ingredient.name(),
                ingredient.description(),
                ingredient.imageUrl(),
                ingredient.price(),
                ingredient.status());
    }
}

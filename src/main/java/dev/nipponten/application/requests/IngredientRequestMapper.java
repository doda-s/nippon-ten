package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Ingredient;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IngredientRequestMapper {

    public Ingredient toModel(Long id, IngredientRequest request) {
        return new Ingredient(
                id,
                request.name(),
                request.description(),
                request.imageUrl(),
                request.price(),
                request.status());
    }
}

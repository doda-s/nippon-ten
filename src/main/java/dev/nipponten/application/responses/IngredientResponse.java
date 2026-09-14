package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Ingredient;
import java.math.BigDecimal;

public record IngredientResponse(
        Long id,
        String name,
        String description,
        String imageUrl,
        BigDecimal price,
        Ingredient.Status status) {}

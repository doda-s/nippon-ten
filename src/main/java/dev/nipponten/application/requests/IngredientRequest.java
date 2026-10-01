package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Ingredient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record IngredientRequest(
        @NotBlank String name,
        String description,
        String imageUrl,
        @NotNull @PositiveOrZero BigDecimal price,
        @NotNull Ingredient.Status status) {}

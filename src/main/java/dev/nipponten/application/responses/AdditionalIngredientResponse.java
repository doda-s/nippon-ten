package dev.nipponten.application.responses;

import dev.nipponten.domain.models.AdditionalIngredient;

public record AdditionalIngredientResponse(
        Long id,
        Long productId,
        Long ingredientId,
        Integer maximumQuantity,
        AdditionalIngredient.Status status) {}

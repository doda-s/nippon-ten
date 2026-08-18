package dev.nipponten.application.requests;

import dev.nipponten.domain.models.AdditionalIngredient;

public record AdditionalIngredientRequest(
        Long ingredientId, Integer maximumQuantity, AdditionalIngredient.Status status) {}

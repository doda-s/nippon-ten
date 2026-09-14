package dev.nipponten.application.requests;

import dev.nipponten.domain.models.AdditionalIngredient;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AdditionalIngredientRequest(
        @NotNull Long ingredientId,
        @NotNull @Positive Integer maximumQuantity,
        @NotNull AdditionalIngredient.Status status) {}

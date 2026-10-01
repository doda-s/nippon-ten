package dev.nipponten.application.requests;

import jakarta.validation.constraints.NotNull;

public record ProductIngredientRequest(@NotNull Long ingredientId) {}

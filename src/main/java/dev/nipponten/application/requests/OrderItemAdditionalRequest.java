package dev.nipponten.application.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemAdditionalRequest(
        @NotNull Long ingredientId, @NotNull @Positive Integer quantity) {}

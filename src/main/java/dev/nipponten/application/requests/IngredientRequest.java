package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Ingredient;
import java.math.BigDecimal;

public record IngredientRequest(String name, String description, String imageUrl, BigDecimal price, Ingredient.Status status) {}

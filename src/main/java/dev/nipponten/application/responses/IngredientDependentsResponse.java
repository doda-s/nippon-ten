package dev.nipponten.application.responses;

import java.util.List;

public record IngredientDependentsResponse(
        List<ProductResponse> products, List<ProductResponse> additionalProducts) {}

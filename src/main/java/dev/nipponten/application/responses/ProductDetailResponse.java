package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Product;
import java.util.List;

public record ProductDetailResponse(
        Long id,
        String name,
        String imageUrl,
        String description,
        Product.Status status,
        List<ProductIngredientResponse> ingredients,
        List<ProductSizeResponse> sizes,
        List<AdditionalIngredientResponse> additionalIngredients) {}

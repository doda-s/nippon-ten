package dev.nipponten.application.responses;

import dev.nipponten.domain.models.AdditionalIngredient;
import dev.nipponten.domain.models.Product;
import dev.nipponten.domain.models.ProductIngredient;
import dev.nipponten.domain.models.ProductSize;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class ProductResponseMapper {

    @Inject ProductIngredientResponseMapper productIngredientMapper;

    @Inject ProductSizeResponseMapper productSizeMapper;

    @Inject AdditionalIngredientResponseMapper additionalIngredientMapper;

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.id(),
                product.name(),
                product.imageUrl(),
                product.description(),
                product.status());
    }

    public ProductDetailResponse toDetailResponse(
            Product product,
            List<ProductIngredient> ingredients,
            List<ProductSize> sizes,
            List<AdditionalIngredient> additionalIngredients) {
        return new ProductDetailResponse(
                product.id(),
                product.name(),
                product.imageUrl(),
                product.description(),
                product.status(),
                ingredients.stream().map(productIngredientMapper::toResponse).toList(),
                sizes.stream().map(productSizeMapper::toResponse).toList(),
                additionalIngredients.stream()
                        .map(additionalIngredientMapper::toResponse)
                        .toList());
    }
}

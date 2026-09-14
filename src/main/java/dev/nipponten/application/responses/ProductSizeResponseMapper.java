package dev.nipponten.application.responses;

import dev.nipponten.domain.models.ProductSize;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProductSizeResponseMapper {

    public ProductSizeResponse toResponse(ProductSize productSize) {
        return new ProductSizeResponse(
                productSize.id(),
                productSize.productId(),
                productSize.price(),
                productSize.status());
    }
}

package dev.nipponten.application.requests;

import dev.nipponten.domain.models.ProductSize;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProductSizeRequestMapper {

    public ProductSize toModel(Long id, Long productId, ProductSizeRequest request) {
        return new ProductSize(id, productId, request.price(), request.status());
    }
}

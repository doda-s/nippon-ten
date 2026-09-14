package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Product;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProductRequestMapper {

    public Product toModel(Long id, ProductRequest request) {
        return new Product(
                id, request.name(), request.imageUrl(), request.description(), request.status());
    }
}

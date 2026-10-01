package dev.nipponten.application.requests;

import dev.nipponten.domain.models.ComboProduct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ComboProductRequestMapper {

    public ComboProduct toModel(Long id, Long comboId, ComboProductRequest request) {
        return new ComboProduct(id, comboId, request.productId());
    }
}

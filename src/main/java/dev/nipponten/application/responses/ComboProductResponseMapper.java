package dev.nipponten.application.responses;

import dev.nipponten.domain.models.ComboProduct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ComboProductResponseMapper {

    public ComboProductResponse toResponse(ComboProduct comboProduct) {
        return new ComboProductResponse(
                comboProduct.id(), comboProduct.comboId(), comboProduct.productId());
    }
}

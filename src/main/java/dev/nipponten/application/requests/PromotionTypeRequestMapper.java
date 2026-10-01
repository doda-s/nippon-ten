package dev.nipponten.application.requests;

import dev.nipponten.domain.models.PromotionType;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PromotionTypeRequestMapper {

    public PromotionType toModel(Long id, PromotionTypeRequest request) {
        return new PromotionType(
                id, request.name(), request.description(), request.type(), request.value());
    }
}

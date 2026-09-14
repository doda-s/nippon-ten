package dev.nipponten.application.responses;

import dev.nipponten.domain.models.PromotionType;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PromotionTypeResponseMapper {

    public PromotionTypeResponse toResponse(PromotionType promotionType) {
        return new PromotionTypeResponse(
                promotionType.id(),
                promotionType.name(),
                promotionType.description(),
                promotionType.type(),
                promotionType.value());
    }
}

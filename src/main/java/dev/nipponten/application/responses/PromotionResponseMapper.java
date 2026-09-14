package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Promotion;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PromotionResponseMapper {

    public PromotionResponse toResponse(Promotion promotion) {
        return new PromotionResponse(
                promotion.id(),
                promotion.title(),
                promotion.price(),
                promotion.imageUrl(),
                promotion.description(),
                promotion.status(),
                promotion.promotionTypeId(),
                promotion.productId(),
                promotion.startDate(),
                promotion.endDate(),
                promotion.enablePromotionPoints());
    }
}

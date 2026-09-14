package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Promotion;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PromotionRequestMapper {

    public Promotion toModel(Long id, PromotionRequest request) {
        return new Promotion(
                id,
                request.title(),
                request.price(),
                request.imageUrl(),
                request.description(),
                request.status(),
                request.promotionTypeId(),
                request.productId(),
                request.startDate(),
                request.endDate(),
                request.enablePromotionPoints());
    }
}

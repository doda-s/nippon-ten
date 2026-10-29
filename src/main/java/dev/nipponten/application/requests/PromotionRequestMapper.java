package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Promotion;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class PromotionRequestMapper {

    public Promotion toModel(Long id, PromotionRequest request) {
        return new Promotion(
                id,
                request.title(),
                request.imageUrl(),
                request.description(),
                request.status(),
                request.promotionTypeId(),
                request.productIds() == null ? List.of() : request.productIds(),
                request.comboIds() == null ? List.of() : request.comboIds(),
                request.startDate(),
                request.endDate(),
                request.enablePromotionPoints());
    }
}

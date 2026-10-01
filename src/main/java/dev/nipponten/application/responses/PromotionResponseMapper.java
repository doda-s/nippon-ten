package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Promotion;
import dev.nipponten.domain.models.PromotionPrice;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class PromotionResponseMapper {

    public PromotionResponse toResponse(Promotion promotion, List<PromotionPrice> prices) {
        return new PromotionResponse(
                promotion.id(),
                promotion.title(),
                prices.stream()
                        .map(
                                price ->
                                        new PromotionPriceResponse(
                                                price.productSizeId(),
                                                price.originalPrice(),
                                                price.promotionalPrice()))
                        .toList(),
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

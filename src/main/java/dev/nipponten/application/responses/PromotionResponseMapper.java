package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Promotion;
import dev.nipponten.domain.models.PromotionPrice;
import dev.nipponten.domain.models.PromotionPricing;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PromotionResponseMapper {

    public PromotionResponse toResponse(Promotion promotion, PromotionPricing pricing) {
        return new PromotionResponse(
                promotion.id(),
                promotion.title(),
                pricing.products().stream().map(this::toProductResponse).toList(),
                pricing.combos().stream().map(this::toComboResponse).toList(),
                promotion.imageUrl(),
                promotion.description(),
                promotion.status(),
                promotion.promotionTypeId(),
                promotion.startDate(),
                promotion.endDate(),
                promotion.enablePromotionPoints());
    }

    private PromotionProductResponse toProductResponse(PromotionPricing.ProductPricing product) {
        return new PromotionProductResponse(
                product.productId(), product.prices().stream().map(this::toPriceResponse).toList());
    }

    private PromotionPriceResponse toPriceResponse(PromotionPrice price) {
        return new PromotionPriceResponse(
                price.productSizeId(), price.originalPrice(), price.promotionalPrice());
    }

    private PromotionComboResponse toComboResponse(PromotionPricing.ComboPricing combo) {
        return new PromotionComboResponse(
                combo.comboId(), combo.originalPrice(), combo.promotionalPrice());
    }
}

package dev.nipponten.domain.models;

import java.math.BigDecimal;
import java.util.List;

// Preço promocional de cada item da promoção: por tamanho, para produtos; único, para combos.
public record PromotionPricing(List<ProductPricing> products, List<ComboPricing> combos) {

    public record ProductPricing(Long productId, List<PromotionPrice> prices) {}

    public record ComboPricing(
            Long comboId, BigDecimal originalPrice, BigDecimal promotionalPrice) {}
}

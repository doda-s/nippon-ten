package dev.nipponten.domain.models;

import java.math.BigDecimal;

public record PromotionPrice(
        Long productSizeId, BigDecimal originalPrice, BigDecimal promotionalPrice) {}

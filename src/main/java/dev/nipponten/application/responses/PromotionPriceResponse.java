package dev.nipponten.application.responses;

import java.math.BigDecimal;

public record PromotionPriceResponse(
        Long productSizeId, BigDecimal originalPrice, BigDecimal promotionalPrice) {}

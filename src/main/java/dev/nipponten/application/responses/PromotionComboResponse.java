package dev.nipponten.application.responses;

import java.math.BigDecimal;

public record PromotionComboResponse(
        Long comboId, BigDecimal originalPrice, BigDecimal promotionalPrice) {}

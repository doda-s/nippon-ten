package dev.nipponten.domain.models;

import java.math.BigDecimal;

public record PromotionType(Long id, String name, String description, Type type, BigDecimal value) {
    public enum Type {
        PERCENTAGE_DISCOUNT,
        FIXED_DISCOUNT
    }
}

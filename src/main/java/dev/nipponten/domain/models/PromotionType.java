package dev.nipponten.domain.models;

import java.math.BigDecimal;

public record PromotionType(
        Long id, String name, String description, String type, BigDecimal value) {}

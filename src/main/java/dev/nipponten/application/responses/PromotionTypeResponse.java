package dev.nipponten.application.responses;

import java.math.BigDecimal;

public record PromotionTypeResponse(
        Long id, String name, String description, String type, BigDecimal value) {}

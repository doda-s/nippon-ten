package dev.nipponten.application.requests;

import java.math.BigDecimal;

public record PromotionTypeRequest(String name, String description, String type, BigDecimal value) {}

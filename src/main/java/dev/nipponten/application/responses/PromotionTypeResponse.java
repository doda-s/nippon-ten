package dev.nipponten.application.responses;

import dev.nipponten.domain.models.PromotionType;
import java.math.BigDecimal;

public record PromotionTypeResponse(
        Long id, String name, String description, PromotionType.Type type, BigDecimal value) {}

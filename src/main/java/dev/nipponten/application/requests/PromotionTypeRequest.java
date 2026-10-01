package dev.nipponten.application.requests;

import dev.nipponten.domain.models.PromotionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PromotionTypeRequest(
        @NotBlank String name,
        String description,
        @NotNull PromotionType.Type type,
        @NotNull @Positive BigDecimal value) {}

package dev.nipponten.application.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PromotionTypeRequest(
        @NotBlank String name,
        String description,
        @NotBlank String type,
        @NotNull BigDecimal value) {}

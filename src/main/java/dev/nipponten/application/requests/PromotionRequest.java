package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Promotion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionRequest(
        @NotBlank String title,
        @NotNull @PositiveOrZero BigDecimal price,
        String imageUrl,
        String description,
        @NotNull Promotion.Status status,
        @NotNull Long promotionTypeId,
        @NotNull Long productId,
        @NotNull LocalDateTime startDate,
        @NotNull LocalDateTime endDate,
        boolean enablePromotionPoints) {}

package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Promotion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

// Produtos e combos são opcionais individualmente; o service exige ao menos um item no total.
public record PromotionRequest(
        @NotBlank String title,
        String imageUrl,
        String description,
        @NotNull Promotion.Status status,
        @NotNull Long promotionTypeId,
        List<@NotNull Long> productIds,
        List<@NotNull Long> comboIds,
        @NotNull LocalDateTime startDate,
        @NotNull LocalDateTime endDate,
        boolean enablePromotionPoints) {}

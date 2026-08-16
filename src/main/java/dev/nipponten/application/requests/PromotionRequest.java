package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Promotion;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionRequest(
        String title,
        BigDecimal price,
        String imageUrl,
        String description,
        Promotion.Status status,
        Long promotionTypeId,
        Long productId,
        LocalDateTime startDate,
        LocalDateTime endDate,
        boolean enablePromotionPoints) {}

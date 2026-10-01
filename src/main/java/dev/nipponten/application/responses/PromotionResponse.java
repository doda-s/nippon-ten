package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Promotion;
import java.time.LocalDateTime;
import java.util.List;

public record PromotionResponse(
        Long id,
        String title,
        List<PromotionPriceResponse> prices,
        String imageUrl,
        String description,
        Promotion.Status status,
        Long promotionTypeId,
        Long productId,
        LocalDateTime startDate,
        LocalDateTime endDate,
        boolean enablePromotionPoints) {}

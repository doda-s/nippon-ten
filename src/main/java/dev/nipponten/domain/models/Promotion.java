package dev.nipponten.domain.models;

import java.time.LocalDateTime;

public record Promotion(
        Long id,
        String title,
        String imageUrl,
        String description,
        Status status,
        Long promotionTypeId,
        Long productId,
        LocalDateTime startDate,
        LocalDateTime endDate,
        boolean enablePromotionPoints) {
    public enum Status {
        ACTIVE,
        INACTIVE
    }
}

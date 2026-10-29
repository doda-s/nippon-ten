package dev.nipponten.domain.models;

import java.time.LocalDateTime;
import java.util.List;

public record Promotion(
        Long id,
        String title,
        String imageUrl,
        String description,
        Status status,
        Long promotionTypeId,
        List<Long> productIds,
        List<Long> comboIds,
        LocalDateTime startDate,
        LocalDateTime endDate,
        boolean enablePromotionPoints) {
    public enum Status {
        ACTIVE,
        INACTIVE
    }
}

package dev.nipponten.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Promotion(Long id, String title, BigDecimal price, String imageUrl, String description, Status status, Long promotionTypeId, Long productId, LocalDateTime startDate, LocalDateTime endDate, boolean enablePromotionPoints) {

    public enum Status { ACTIVE, INACTIVE }
}

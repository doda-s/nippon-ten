package dev.nipponten.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Combo(
        Long id,
        String name,
        BigDecimal price,
        String imageUrl,
        String description,
        Status status,
        LocalDateTime startDate,
        LocalDateTime endDate) {

    public enum Status {
        ACTIVE,
        INACTIVE
    }
}

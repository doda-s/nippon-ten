package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Combo;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ComboResponse(
        Long id,
        String name,
        BigDecimal price,
        String imageUrl,
        String description,
        Combo.Status status,
        LocalDateTime startDate,
        LocalDateTime endDate) {}

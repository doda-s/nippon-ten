package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Combo;
import java.math.BigDecimal;

public record ComboRequest(
        String name, BigDecimal price, String imageUrl, String description, Combo.Status status) {}

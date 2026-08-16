package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Combo;
import java.math.BigDecimal;

public record ComboResponse(Long id, String name, BigDecimal price, String imageUrl, String description, Combo.Status status) {}

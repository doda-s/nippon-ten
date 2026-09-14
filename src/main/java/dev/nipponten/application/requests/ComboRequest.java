package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Combo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ComboRequest(
        @NotBlank String name,
        @NotNull @Positive BigDecimal price,
        String imageUrl,
        String description,
        @NotNull Combo.Status status) {}

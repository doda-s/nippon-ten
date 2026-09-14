package dev.nipponten.application.requests;

import dev.nipponten.domain.models.ProductSize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record ProductSizeRequest(
        @NotNull @PositiveOrZero BigDecimal price, @NotNull ProductSize.Status status) {}

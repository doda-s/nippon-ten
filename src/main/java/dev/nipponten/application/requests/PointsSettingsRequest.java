package dev.nipponten.application.requests;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

// earnRate = 0 desliga o acúmulo; expirationDays ausente = pontos não expiram.
public record PointsSettingsRequest(
        @NotNull @PositiveOrZero @Digits(integer = 6, fraction = 4) BigDecimal earnRate,
        @NotNull @Positive @Digits(integer = 36, fraction = 2) BigDecimal pointValue,
        @Positive Integer expirationDays) {}

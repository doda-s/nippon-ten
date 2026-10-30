package dev.nipponten.application.requests;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

// from e to são dias de negócio, inclusivos.
public record PointsSimulationRequest(
        @NotNull @PositiveOrZero @Digits(integer = 6, fraction = 4) BigDecimal earnRate,
        @NotNull @Positive @Digits(integer = 36, fraction = 2) BigDecimal pointValue,
        @NotNull LocalDate from,
        @NotNull LocalDate to) {}

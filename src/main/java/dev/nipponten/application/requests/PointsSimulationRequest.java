package dev.nipponten.application.requests;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

// amount é o valor de uma compra, pago em dinheiro e sem a taxa de entrega (a base do crédito).
public record PointsSimulationRequest(
        @NotNull @PositiveOrZero @Digits(integer = 6, fraction = 4) BigDecimal earnRate,
        @NotNull @Positive @Digits(integer = 36, fraction = 2) BigDecimal pointValue,
        @NotNull @PositiveOrZero @Digits(integer = 36, fraction = 2) BigDecimal amount) {}

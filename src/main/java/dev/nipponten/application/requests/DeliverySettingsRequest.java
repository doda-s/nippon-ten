package dev.nipponten.application.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

// A maior faixa é o raio de entrega. A ordem das faixas no corpo não importa.
public record DeliverySettingsRequest(
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double storeLatitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double storeLongitude,
        @NotEmpty @Size(max = 20) List<@NotNull @Valid Tier> tiers) {

    public record Tier(
            @NotNull @Positive @Digits(integer = 3, fraction = 2) BigDecimal maxDistanceKm,
            @NotNull @PositiveOrZero @Digits(integer = 36, fraction = 2) BigDecimal fee) {}
}

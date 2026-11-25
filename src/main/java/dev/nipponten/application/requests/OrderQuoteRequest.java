package dev.nipponten.application.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

// pointsToRedeem exige login de cliente: a cotação confere o saldo.
public record OrderQuoteRequest(
        @NotEmpty List<@NotNull @Valid OrderItemRequest> items,
        @PositiveOrZero Integer pointsToRedeem) {

    public int points() {
        return pointsToRedeem == null ? 0 : pointsToRedeem;
    }
}

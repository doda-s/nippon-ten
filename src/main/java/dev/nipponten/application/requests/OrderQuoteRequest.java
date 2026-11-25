package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Order;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

// pointsToRedeem exige login de cliente: a cotação confere o saldo. Com fulfillmentType =
// DELIVERY e um endereço (addressId do cliente logado ou address), a cotação inclui a taxa de
// entrega; sem eles, a taxa é 0.
public record OrderQuoteRequest(
        @NotEmpty List<@NotNull @Valid OrderItemRequest> items,
        @PositiveOrZero Integer pointsToRedeem,
        Order.FulfillmentType fulfillmentType,
        Long addressId,
        @Valid OrderAddressRequest address) {

    public int points() {
        return pointsToRedeem == null ? 0 : pointsToRedeem;
    }
}

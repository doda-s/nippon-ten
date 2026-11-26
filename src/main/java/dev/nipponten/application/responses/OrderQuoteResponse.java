package dev.nipponten.application.responses;

import java.math.BigDecimal;
import java.util.List;

// deliveryDistanceKm é nulo sem entrega ou com o cálculo da taxa desligado.
public record OrderQuoteResponse(
        List<OrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal deliveryFee,
        BigDecimal deliveryDistanceKm,
        BigDecimal total,
        int pointsRedeemed,
        BigDecimal pointsEligibleSubtotal,
        int pointsToEarn) {}

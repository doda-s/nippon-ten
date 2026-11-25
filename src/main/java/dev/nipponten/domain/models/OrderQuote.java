package dev.nipponten.domain.models;

import java.math.BigDecimal;
import java.util.List;

// pointsEligibleSubtotal é o teto do desconto em pontos; pointsToEarn, quantos pontos o pedido
// renderia ao ser concluído (só vale para cliente com conta).
public record OrderQuote(
        List<OrderItem> items,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal deliveryFee,
        BigDecimal total,
        int pointsRedeemed,
        BigDecimal pointsEligibleSubtotal,
        int pointsToEarn) {}

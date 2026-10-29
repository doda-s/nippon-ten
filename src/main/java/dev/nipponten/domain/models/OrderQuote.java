package dev.nipponten.domain.models;

import java.math.BigDecimal;
import java.util.List;

public record OrderQuote(
        List<OrderItem> items,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal deliveryFee,
        BigDecimal total) {}

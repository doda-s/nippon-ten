package dev.nipponten.application.responses;

import java.math.BigDecimal;
import java.util.List;

public record OrderQuoteResponse(
        List<OrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal deliveryFee,
        BigDecimal total) {}

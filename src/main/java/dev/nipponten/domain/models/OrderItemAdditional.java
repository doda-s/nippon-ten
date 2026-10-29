package dev.nipponten.domain.models;

import java.math.BigDecimal;

// quantity é por unidade do item: 2 de queijo extra em cada pastel.
public record OrderItemAdditional(
        Long id, Long ingredientId, String name, int quantity, BigDecimal unitPrice) {}

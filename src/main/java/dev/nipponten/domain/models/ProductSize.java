package dev.nipponten.domain.models;

import java.math.BigDecimal;

public record ProductSize(Long id, Long productId, BigDecimal price, Status status) {

    public enum Status { ACTIVE, INACTIVE }
}

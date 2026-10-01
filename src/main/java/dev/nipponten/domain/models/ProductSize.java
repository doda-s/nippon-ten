package dev.nipponten.domain.models;

import java.math.BigDecimal;

public record ProductSize(Long id, Long productId, Long sizeId, BigDecimal price, Status status) {

    public enum Status {
        ACTIVE,
        INACTIVE
    }
}

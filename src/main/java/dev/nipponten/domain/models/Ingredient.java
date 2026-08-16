package dev.nipponten.domain.models;

import java.math.BigDecimal;

public record Ingredient(Long id, String name, String description, String imageUrl, BigDecimal price, Status status) {

    public enum Status { ACTIVE, OUT_OF_STOCK }
}

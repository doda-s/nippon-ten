package dev.nipponten.domain.models;

import java.math.BigDecimal;

public record Combo(Long id, String name, BigDecimal price, String imageUrl, String description, Status status) {

    public enum Status { ACTIVE, INACTIVE }
}

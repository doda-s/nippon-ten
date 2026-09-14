package dev.nipponten.domain.models;

public record Product(Long id, String name, String imageUrl, String description, Status status) {

    public enum Status {
        ACTIVE,
        INACTIVE
    }
}

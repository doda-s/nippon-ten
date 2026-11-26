package dev.nipponten.domain.models;

// categoryId é opcional: produto sem categoria aparece numa seção à parte no cardápio.
public record Product(
        Long id, String name, String imageUrl, String description, Status status, Long categoryId) {

    public enum Status {
        ACTIVE,
        INACTIVE
    }
}

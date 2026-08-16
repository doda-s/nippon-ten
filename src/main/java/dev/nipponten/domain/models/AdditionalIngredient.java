package dev.nipponten.domain.models;

public record AdditionalIngredient(Long id, Long productId, Long ingredientId, Integer maximumQuantity, Status status) {

    public enum Status { ACTIVE, INACTIVE }
}

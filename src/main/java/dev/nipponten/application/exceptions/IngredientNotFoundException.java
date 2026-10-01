package dev.nipponten.application.exceptions;

public class IngredientNotFoundException extends NotFoundException {
    public IngredientNotFoundException(Long id) {
        super("Ingredient not found: " + id);
    }
}

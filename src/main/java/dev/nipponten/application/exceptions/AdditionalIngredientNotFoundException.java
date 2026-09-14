package dev.nipponten.application.exceptions;

public class AdditionalIngredientNotFoundException extends NotFoundException {
    public AdditionalIngredientNotFoundException(Long id) {
        super("AdditionalIngredient not found: " + id);
    }
}

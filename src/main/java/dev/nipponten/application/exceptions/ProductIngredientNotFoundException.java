package dev.nipponten.application.exceptions;

public class ProductIngredientNotFoundException extends NotFoundException {
    public ProductIngredientNotFoundException(Long id) {
        super("ProductIngredient not found: " + id);
    }
}

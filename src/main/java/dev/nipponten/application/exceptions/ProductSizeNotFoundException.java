package dev.nipponten.application.exceptions;

public class ProductSizeNotFoundException extends NotFoundException {
    public ProductSizeNotFoundException(Long id) {
        super("ProductSize not found: " + id);
    }
}

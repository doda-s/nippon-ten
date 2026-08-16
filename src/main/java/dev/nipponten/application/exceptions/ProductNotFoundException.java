package dev.nipponten.application.exceptions;

public class ProductNotFoundException extends NotFoundException {
    public ProductNotFoundException(Long id) {
        super("Product not found: " + id);
    }
}

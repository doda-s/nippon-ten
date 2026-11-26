package dev.nipponten.application.exceptions;

public class CategoryNotFoundException extends NotFoundException {
    public CategoryNotFoundException(Long id) {
        super("Category not found: " + id);
    }
}

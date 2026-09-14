package dev.nipponten.application.exceptions;

public class ComboNotFoundException extends NotFoundException {
    public ComboNotFoundException(Long id) {
        super("Combo not found: " + id);
    }
}

package dev.nipponten.application.exceptions;

public class InternalNotFoundException extends NotFoundException {
    public InternalNotFoundException(Long id) {
        super("Internal not found: " + id);
    }
}

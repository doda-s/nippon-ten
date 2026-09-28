package dev.nipponten.application.exceptions;

public class SizeNotFoundException extends NotFoundException {
    public SizeNotFoundException(Long id) {
        super("Size not found: " + id);
    }
}

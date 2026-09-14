package dev.nipponten.application.exceptions;

public class ComboProductNotFoundException extends NotFoundException {
    public ComboProductNotFoundException(Long id) {
        super("ComboProduct not found: " + id);
    }
}

package dev.nipponten.application.exceptions;

public class ClientNotFoundException extends NotFoundException {
    public ClientNotFoundException(Long id) {
        super("Client not found: " + id);
    }
}

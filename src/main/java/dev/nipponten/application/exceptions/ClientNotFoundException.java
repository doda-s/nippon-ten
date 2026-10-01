package dev.nipponten.application.exceptions;

public class ClientNotFoundException extends NotFoundException {
    public ClientNotFoundException(Long id) {
        super("Client not found: " + id);
    }

    private ClientNotFoundException(String message) {
        super(message);
    }

    public static ClientNotFoundException forUser(Long userId) {
        return new ClientNotFoundException("Client not found for user: " + userId);
    }
}

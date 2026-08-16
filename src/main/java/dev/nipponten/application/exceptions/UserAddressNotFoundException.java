package dev.nipponten.application.exceptions;

public class UserAddressNotFoundException extends NotFoundException {
    public UserAddressNotFoundException(Long id) {
        super("UserAddress not found: " + id);
    }
}

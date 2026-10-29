package dev.nipponten.application.exceptions;

public class OrderNotFoundException extends NotFoundException {
    public OrderNotFoundException(Long id) {
        super("Order not found: " + id);
    }
}

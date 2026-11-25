package dev.nipponten.application.exceptions;

public class OrderNotFoundException extends NotFoundException {
    public OrderNotFoundException(Long id) {
        super("Order not found: " + id);
    }

    public OrderNotFoundException(String code) {
        super("Order not found: " + code);
    }
}

package dev.nipponten.application.exceptions;

public class PromotionNotFoundException extends NotFoundException {
    public PromotionNotFoundException(Long id) {
        super("Promotion not found: " + id);
    }
}

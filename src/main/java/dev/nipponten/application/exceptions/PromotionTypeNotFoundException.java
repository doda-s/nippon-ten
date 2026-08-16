package dev.nipponten.application.exceptions;

public class PromotionTypeNotFoundException extends NotFoundException {
    public PromotionTypeNotFoundException(Long id) {
        super("PromotionType not found: " + id);
    }
}

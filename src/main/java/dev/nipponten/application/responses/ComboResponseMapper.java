package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Combo;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ComboResponseMapper {

    public ComboResponse toResponse(Combo combo) {
        return new ComboResponse(
                combo.id(),
                combo.name(),
                combo.price(),
                combo.imageUrl(),
                combo.description(),
                combo.status());
    }
}

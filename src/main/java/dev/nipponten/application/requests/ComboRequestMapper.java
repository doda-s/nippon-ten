package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Combo;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ComboRequestMapper {

    public Combo toModel(Long id, ComboRequest request) {
        return new Combo(
                id,
                request.name(),
                request.price(),
                request.imageUrl(),
                request.description(),
                request.status());
    }
}

package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Size;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SizeRequestMapper {

    public Size toModel(Long id, SizeRequest request) {
        return new Size(id, request.name());
    }
}

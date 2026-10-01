package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Size;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SizeResponseMapper {

    public SizeResponse toResponse(Size size) {
        return new SizeResponse(size.id(), size.name());
    }
}

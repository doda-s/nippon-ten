package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Internal;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InternalRequestMapper {

    public Internal toModel(Long id, InternalRequest request) {
        return new Internal(
                id,
                request.userId(),
                request.internalRoleId(),
                request.name(),
                request.lastName(),
                request.cpf());
    }
}

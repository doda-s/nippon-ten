package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Internal;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InternalResponseMapper {

    public InternalResponse toResponse(Internal internal) {
        return new InternalResponse(
                internal.id(),
                internal.userId(),
                internal.internalRoleId(),
                internal.name(),
                internal.lastName(),
                internal.cpf());
    }
}

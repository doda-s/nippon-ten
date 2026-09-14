package dev.nipponten.application.responses;

import dev.nipponten.domain.models.InternalRole;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InternalRoleResponseMapper {

    public InternalRoleResponse toResponse(InternalRole internalRole) {
        return new InternalRoleResponse(internalRole.id(), internalRole.name());
    }
}

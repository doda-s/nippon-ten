package dev.nipponten.application.requests;

import dev.nipponten.domain.models.InternalRole;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InternalRoleRequestMapper {

    public InternalRole toModel(Long id, InternalRoleRequest request) {
        return new InternalRole(id, request.name());
    }
}

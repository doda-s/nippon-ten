package dev.nipponten.application.responses;

import dev.nipponten.domain.models.InternalPermission;
import java.util.List;

public record InternalResponse(
        Long id,
        Long userId,
        Long internalRoleId,
        List<InternalPermission> userPermissions,
        String name,
        String lastName,
        String cpf) {}

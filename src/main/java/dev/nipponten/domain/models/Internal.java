package dev.nipponten.domain.models;

import java.util.List;

public record Internal(
        Long id,
        Long userId,
        Long internalRoleId,
        List<InternalPermission> userPermissions,
        String name,
        String lastName,
        String cpf) {}

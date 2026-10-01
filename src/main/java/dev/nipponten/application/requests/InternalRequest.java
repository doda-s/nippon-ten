package dev.nipponten.application.requests;

import java.util.List;

import dev.nipponten.domain.models.InternalPermission;
import jakarta.validation.constraints.NotBlank;

public record InternalRequest(
        Long internalRoleId,
        List<InternalPermission> userPermissions,
        @NotBlank String name,
        @NotBlank String lastName,
        @NotBlank String cpf) {}

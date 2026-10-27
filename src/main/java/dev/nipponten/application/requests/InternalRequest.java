package dev.nipponten.application.requests;

import dev.nipponten.domain.models.InternalPermission;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record InternalRequest(
        Long internalRoleId,
        List<InternalPermission> userPermissions,
        @NotBlank String name,
        @NotBlank String lastName,
        @NotBlank String cpf) {}

package dev.nipponten.application.requests;

import dev.nipponten.domain.models.InternalPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record InternalRoleRequest(
        @NotBlank String name,
        @NotNull List<InternalPermission> permissions,
        boolean defaultRole) {}

package dev.nipponten.application.requests;

import java.util.List;

import dev.nipponten.domain.models.InternalPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InternalRoleRequest(
        @NotBlank String name,
        @NotNull List<InternalPermission> permissions,
        boolean defaultRole) {}

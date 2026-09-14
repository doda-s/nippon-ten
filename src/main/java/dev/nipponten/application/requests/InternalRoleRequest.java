package dev.nipponten.application.requests;

import jakarta.validation.constraints.NotBlank;

public record InternalRoleRequest(@NotBlank String name) {}

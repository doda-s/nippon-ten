package dev.nipponten.application.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InternalRequest(
        @NotNull Long internalRoleId,
        @NotBlank String name,
        @NotBlank String lastName,
        @NotBlank String cpf) {}

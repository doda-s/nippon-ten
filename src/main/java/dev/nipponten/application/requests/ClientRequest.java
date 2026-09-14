package dev.nipponten.application.requests;

import jakarta.validation.constraints.NotBlank;

public record ClientRequest(
        @NotBlank String name, @NotBlank String lastName, String cpf, Integer promotionPoints) {}

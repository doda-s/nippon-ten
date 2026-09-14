package dev.nipponten.application.requests;

import jakarta.validation.constraints.NotBlank;

public record UserAddressRequest(
        @NotBlank String streetAddress,
        @NotBlank String number,
        @NotBlank String cep,
        String complement) {}

package dev.nipponten.application.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// Bairro, cidade e UF não vêm no corpo: saem da consulta do CEP.
public record UserAddressRequest(
        @NotBlank String streetAddress,
        @NotBlank String number,
        @NotBlank @Pattern(regexp = CepFormat.REGEXP, message = CepFormat.MESSAGE) String cep,
        String complement) {}

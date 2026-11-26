package dev.nipponten.application.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Endereço da loja, digitado como o do cliente. Bairro, cidade e UF saem da consulta do CEP.
public record StoreAddressRequest(
        @NotBlank @Size(max = 255) String streetAddress,
        @NotBlank @Size(max = 255) String number,
        @NotBlank @Pattern(regexp = CepFormat.REGEXP, message = CepFormat.MESSAGE) String cep,
        @Size(max = 255) String complement) {}

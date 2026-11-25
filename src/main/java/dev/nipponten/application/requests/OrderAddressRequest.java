package dev.nipponten.application.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Endereço digitado no pedido (visitante ou funcionário); o cliente logado usa addressId. Bairro,
// cidade e UF saem da consulta do CEP.
public record OrderAddressRequest(
        @NotBlank @Size(max = 255) String streetAddress,
        @NotBlank @Size(max = 255) String number,
        @NotBlank @Pattern(regexp = CepFormat.REGEXP, message = CepFormat.MESSAGE) String cep,
        @Size(max = 255) String complement) {}

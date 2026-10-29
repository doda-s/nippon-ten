package dev.nipponten.application.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Endereço digitado no pedido (visitante ou funcionário); o cliente logado usa addressId.
public record OrderAddressRequest(
        @NotBlank @Size(max = 255) String streetAddress,
        @NotBlank @Size(max = 255) String number,
        @NotBlank @Size(max = 255) String cep,
        @Size(max = 255) String complement) {}

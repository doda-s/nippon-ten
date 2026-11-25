package dev.nipponten.application.responses;

public record UserAddressResponse(
        Long id,
        Long clientId,
        String streetAddress,
        String number,
        String cep,
        String complement,
        String neighborhood,
        String city,
        String state) {}

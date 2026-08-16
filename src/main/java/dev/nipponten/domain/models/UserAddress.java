package dev.nipponten.domain.models;

public record UserAddress(
        Long id,
        Long clientId,
        String streetAddress,
        String number,
        String cep,
        String complement) {}

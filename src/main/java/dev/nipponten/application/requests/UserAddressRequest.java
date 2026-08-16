package dev.nipponten.application.requests;

public record UserAddressRequest(
        Long clientId, String streetAddress, String number, String cep, String complement) {}

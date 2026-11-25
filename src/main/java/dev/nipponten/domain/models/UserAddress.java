package dev.nipponten.domain.models;

// neighborhood, city e state vêm da consulta do CEP; nulos nos endereços salvos antes dela.
public record UserAddress(
        Long id,
        Long clientId,
        String streetAddress,
        String number,
        String cep,
        String complement,
        String neighborhood,
        String city,
        String state) {

    public Address toAddress() {
        return new Address(streetAddress, number, complement, cep, neighborhood, city, state);
    }
}

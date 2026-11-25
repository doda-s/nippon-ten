package dev.nipponten.domain.models;

/**
 * Endereço de entrega. O que o cliente digita é rua, número, complemento e CEP; bairro, cidade e UF
 * saem da consulta do CEP e ficam nulos até ela.
 */
public record Address(
        String streetAddress,
        String number,
        String complement,
        String cep,
        String neighborhood,
        String city,
        String state) {

    public static final Address NONE = new Address(null, null, null, null, null, null, null);
}

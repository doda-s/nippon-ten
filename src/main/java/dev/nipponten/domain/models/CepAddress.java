package dev.nipponten.domain.models;

import java.time.Instant;

/**
 * O que a consulta de CEP devolve. cep tem só os 8 dígitos. street e neighborhood são nulos no CEP
 * único de cidade pequena, que não aponta rua.
 */
public record CepAddress(
        String cep,
        String street,
        String neighborhood,
        String city,
        String state,
        Instant fetchedAt) {}

package dev.nipponten.application.requests;

public record ClientRequest(
        Long userId, String name, String lastName, String cpf, Integer promotionPoints) {}

package dev.nipponten.domain.models;

public record Client(
        Long id, Long userId, String name, String lastName, String cpf, Integer promotionPoints) {}

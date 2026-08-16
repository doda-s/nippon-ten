package dev.nipponten.application.responses;

public record ClientResponse(Long id, Long userId, String name, String lastName, String cpf, Integer promotionPoints) {}

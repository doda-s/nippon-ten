package dev.nipponten.application.requests;

public record InternalRequest(
        Long userId, Long internalRoleId, String name, String lastName, String cpf) {}

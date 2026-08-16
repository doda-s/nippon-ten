package dev.nipponten.application.responses;

public record InternalResponse(
        Long id, Long userId, Long internalRoleId, String name, String lastName, String cpf) {}

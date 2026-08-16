package dev.nipponten.domain.models;

public record Internal(Long id, Long userId, Long internalRoleId, String name, String lastName, String cpf) {}

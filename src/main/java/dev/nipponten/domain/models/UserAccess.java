package dev.nipponten.domain.models;

import java.util.Set;

/**
 * Dados de um usuário ativo que decidem o acesso dele, lidos numa consulta só. {@code roleActive} e
 * {@code rolePermissions} vêm vazios para cliente ou interno sem role.
 */
public record UserAccess(
        Long userId,
        String email,
        UserType type,
        Set<InternalPermission> userPermissions,
        boolean roleActive,
        Set<InternalPermission> rolePermissions) {}

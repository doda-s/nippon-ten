package dev.nipponten.domain.models;

import java.util.Set;

/**
 * Usuário autenticado com as permissões efetivas já resolvidas (role ativa + individuais). {@code
 * superAdmin} indica que a role do interno é a reservada.
 */
public record AuthenticatedUser(
        Long userId,
        String email,
        UserType type,
        Set<InternalPermission> permissions,
        boolean superAdmin) {

    public boolean has(InternalPermission permission) {
        return permissions.contains(permission);
    }
}

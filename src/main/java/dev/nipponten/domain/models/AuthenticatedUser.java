package dev.nipponten.domain.models;

import java.util.Set;

/** Usuário autenticado com as permissões efetivas já resolvidas (role ativa + individuais). */
public record AuthenticatedUser(
        Long userId, String email, UserType type, Set<InternalPermission> permissions) {

    public boolean has(InternalPermission permission) {
        return permissions.contains(permission);
    }
}

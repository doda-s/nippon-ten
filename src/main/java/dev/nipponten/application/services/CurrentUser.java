package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.ForbiddenException;
import dev.nipponten.domain.models.AuthenticatedUser;
import dev.nipponten.domain.models.InternalPermission;

/** Usuário do request atual. A implementação lê a identidade de segurança em infrastructure. */
public interface CurrentUser {

    /** Lança {@link dev.nipponten.application.exceptions.UnauthorizedException} se anônimo. */
    AuthenticatedUser get();

    /**
     * Libera quando o usuário do request é o próprio {@code userId} ou tem a permissão; senão 403.
     * Roda antes de buscar o recurso, então um id inexistente também dá 403 para quem não pode.
     */
    default void requireSelfOr(InternalPermission permission, Long userId) {
        AuthenticatedUser user = get();
        if (!user.userId().equals(userId) && !user.has(permission)) {
            throw new ForbiddenException("Not allowed to access user " + userId);
        }
    }
}

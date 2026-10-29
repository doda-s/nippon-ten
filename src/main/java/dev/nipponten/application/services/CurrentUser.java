package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.ForbiddenException;
import dev.nipponten.domain.models.AuthenticatedUser;
import dev.nipponten.domain.models.InternalPermission;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Usuário do request atual. A implementação lê a identidade de segurança em infrastructure. */
public interface CurrentUser {

    /** Lança {@link dev.nipponten.application.exceptions.UnauthorizedException} se anônimo. */
    AuthenticatedUser get();

    /** Vazio se o request é anônimo, para as rotas públicas que mudam com quem chama. */
    Optional<AuthenticatedUser> find();

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

    /**
     * Ninguém concede o que não tem: 403 se faltar ao usuário do request alguma das permissões. O
     * super_admin passa sempre. Aceita nulo como vazio.
     */
    default void requireHolds(Collection<InternalPermission> permissions) {
        AuthenticatedUser user = get();
        if (permissions == null || user.superAdmin()) {
            return;
        }
        List<InternalPermission> missing =
                permissions.stream().filter(p -> !user.has(p)).distinct().sorted().toList();
        if (!missing.isEmpty()) {
            throw new ForbiddenException("Cannot grant permissions you do not have: " + missing);
        }
    }
}

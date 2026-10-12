package dev.nipponten.application.services;

import dev.nipponten.domain.models.AuthenticatedUser;
import dev.nipponten.domain.models.InternalPermission;
import dev.nipponten.domain.models.UserAccess;
import dev.nipponten.domain.repositories.UserAccessRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Calcula as permissões efetivas de um usuário a partir do banco: assim desativar um usuário ou uma
 * role vale na hora, sem esperar o token expirar.
 */
@ApplicationScoped
public class PermissionResolver {

    @Inject UserAccessRepository repository;

    @Inject AuthenticationCache cache;

    @Inject Clock clock;

    /** Login e refresh. Vazio quando o usuário não existe mais ou está desativado. */
    public Optional<AuthenticatedUser> resolve(Long userId) {
        return repository.findActiveUser(userId).map(PermissionResolver::toAuthenticated);
    }

    /**
     * Cada request autenticado. Vazio também quando a sessão do token foi encerrada. Sessão e
     * permissões saem da mesma consulta, e o resultado fica no {@link AuthenticationCache}.
     */
    public Optional<AuthenticatedUser> resolveSession(Long userId, UUID sessionId) {
        return cache.get(
                userId,
                sessionId,
                () ->
                        repository
                                .findActiveSession(userId, sessionId, Instant.now(clock))
                                .map(PermissionResolver::toAuthenticated));
    }

    // Role desativada perde efeito, mas as permissões individuais do interno continuam valendo.
    private static AuthenticatedUser toAuthenticated(UserAccess access) {
        Set<InternalPermission> permissions = EnumSet.noneOf(InternalPermission.class);
        permissions.addAll(access.userPermissions());
        if (access.roleActive()) {
            permissions.addAll(access.rolePermissions());
        }
        return new AuthenticatedUser(
                access.userId(),
                access.email(),
                access.type(),
                Set.copyOf(permissions),
                access.roleActive() && access.roleReserved());
    }
}

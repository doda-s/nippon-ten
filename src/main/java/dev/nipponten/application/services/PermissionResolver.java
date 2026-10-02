package dev.nipponten.application.services;

import dev.nipponten.domain.models.AuthenticatedUser;
import dev.nipponten.domain.models.Internal;
import dev.nipponten.domain.models.InternalPermission;
import dev.nipponten.domain.models.InternalRole;
import dev.nipponten.domain.models.User;
import dev.nipponten.domain.models.UserType;
import dev.nipponten.domain.repositories.InternalRepository;
import dev.nipponten.domain.repositories.InternalRoleRepository;
import dev.nipponten.domain.repositories.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Calcula as permissões efetivas de um usuário a partir do banco, a cada request: assim desativar
 * um usuário ou uma role vale na hora, sem esperar o token expirar.
 */
@ApplicationScoped
public class PermissionResolver {

    @Inject UserRepository userRepository;

    @Inject InternalRepository internalRepository;

    @Inject InternalRoleRepository internalRoleRepository;

    /** Vazio quando o usuário não existe mais ou está desativado. */
    public Optional<AuthenticatedUser> resolve(Long userId) {
        User user = userRepository.getById(userId);
        if (user == null || !user.active()) {
            return Optional.empty();
        }
        List<Internal> internals = internalRepository.getByUser(userId);
        if (internals.isEmpty()) {
            return Optional.of(
                    new AuthenticatedUser(userId, user.email(), UserType.CLIENT, Set.of()));
        }
        return Optional.of(
                new AuthenticatedUser(
                        userId,
                        user.email(),
                        UserType.INTERNAL,
                        effectivePermissions(internals.getFirst())));
    }

    // Role desativada perde efeito, mas as permissões individuais do interno continuam valendo.
    private Set<InternalPermission> effectivePermissions(Internal internal) {
        Set<InternalPermission> permissions = EnumSet.noneOf(InternalPermission.class);
        if (internal.userPermissions() != null) {
            permissions.addAll(internal.userPermissions());
        }
        InternalRole role =
                internal.internalRoleId() == null
                        ? null
                        : internalRoleRepository.getById(internal.internalRoleId());
        if (role != null && role.active() && role.permissions() != null) {
            permissions.addAll(role.permissions());
        }
        return Set.copyOf(permissions);
    }
}

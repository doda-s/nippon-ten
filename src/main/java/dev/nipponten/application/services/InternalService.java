package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.ForbiddenException;
import dev.nipponten.application.exceptions.InternalNotFoundException;
import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.domain.models.AuthenticatedUser;
import dev.nipponten.domain.models.Internal;
import dev.nipponten.domain.models.InternalPermission;
import dev.nipponten.domain.models.InternalRole;
import dev.nipponten.domain.repositories.ClientRepository;
import dev.nipponten.domain.repositories.InternalRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@ApplicationScoped
public class InternalService {

    @Inject InternalRepository repository;

    // Repositório, e não o service, para não criar ciclo InternalService <-> ClientService.
    @Inject ClientRepository clientRepository;

    @Inject InternalRoleService internalRoleService;

    @Inject AuthenticationCache authenticationCache;

    @Inject CurrentUser currentUser;

    public Internal create(Internal model) {
        InternalRole role =
                model.internalRoleId() != null
                        ? internalRoleService.getById(model.internalRoleId())
                        : internalRoleService.getDefault();
        Long roleId = role.id();
        requireSuperAdminFor(role);
        // Ninguém concede o que não tem: nem pela role, nem pelas permissões individuais.
        currentUser.requireHolds(role.permissions());
        currentUser.requireHolds(model.userPermissions());
        requireNotClient(model.userId());
        return repository.save(
                new Internal(
                        model.id(),
                        model.userId(),
                        roleId,
                        model.userPermissions(),
                        model.name(),
                        model.lastName(),
                        model.cpf()));
    }

    public Internal getById(Long id) {
        Internal model = repository.getById(id);
        if (model == null) throw new InternalNotFoundException(id);
        return model;
    }

    public List<Internal> getAll() {
        return repository.getAll();
    }

    public Internal update(Long id, Internal model) {
        Internal current = getById(id);
        InternalRole role = internalRoleService.getById(model.internalRoleId());
        requireSuperAdminFor(role);
        if (current.internalRoleId() != null) {
            requireSuperAdminFor(internalRoleService.getById(current.internalRoleId()));
        }
        boolean roleChanged = !Objects.equals(current.internalRoleId(), model.internalRoleId());
        Set<InternalPermission> granted = toSet(model.userPermissions());
        granted.removeAll(toSet(current.userPermissions()));
        boolean permissionsChanged =
                !toSet(model.userPermissions()).equals(toSet(current.userPermissions()));
        AuthenticatedUser actor = currentUser.get();
        if (actor.userId().equals(current.userId()) && (roleChanged || permissionsChanged)) {
            throw new ForbiddenException("Cannot change your own role or permissions");
        }
        // Só o que muda precisa ser coberto por quem edita: um interno pode já ter permissões que o
        // editor não tem, e editar o nome dele não as concede de novo.
        if (roleChanged) {
            currentUser.requireHolds(role.permissions());
        }
        currentUser.requireHolds(granted);
        authenticationCache.invalidateUser(current.userId());
        return repository.save(
                new Internal(
                        id,
                        current.userId(),
                        model.internalRoleId(),
                        model.userPermissions(),
                        model.name(),
                        model.lastName(),
                        model.cpf()));
    }

    public void delete(Long id) {
        Internal model = getById(id);
        authenticationCache.invalidateUser(model.userId());
        repository.remove(model);
    }

    // Dar, tirar ou mexer num interno com a role reservada é só para super_admin. Sem token, o
    // currentUser.get() já responde 401.
    private void requireSuperAdminFor(InternalRole role) {
        if (role.reserved() && !currentUser.get().superAdmin()) {
            throw new ForbiddenException(
                    "Only a super admin can manage internal users with role " + role.name());
        }
    }

    private static Set<InternalPermission> toSet(List<InternalPermission> permissions) {
        Set<InternalPermission> set = EnumSet.noneOf(InternalPermission.class);
        if (permissions != null) {
            set.addAll(permissions);
        }
        return set;
    }

    /**
     * Verifica se um User é cliente ou interno, e nunca os dois.
     *
     * @param userId
     */
    private void requireNotClient(Long userId) {
        if (!clientRepository.getByUser(userId).isEmpty()) {
            throw new InvalidRequestException(
                    "User " + userId + " is already a client and cannot become an internal user");
        }
    }

    public List<Internal> getByUser(Long userId) {
        return repository.getByUser(userId);
    }

    public List<Internal> getByInternalRole(Long internalRoleId) {
        return repository.getByInternalRole(internalRoleId);
    }
}

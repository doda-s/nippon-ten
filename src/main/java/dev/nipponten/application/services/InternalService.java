package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.ForbiddenException;
import dev.nipponten.application.exceptions.InternalNotFoundException;
import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.domain.models.Internal;
import dev.nipponten.domain.models.InternalRole;
import dev.nipponten.domain.repositories.ClientRepository;
import dev.nipponten.domain.repositories.InternalRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

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
        requireSuperAdminFor(internalRoleService.getById(model.internalRoleId()));
        if (current.internalRoleId() != null) {
            requireSuperAdminFor(internalRoleService.getById(current.internalRoleId()));
        }
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

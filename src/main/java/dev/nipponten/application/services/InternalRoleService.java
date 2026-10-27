package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.InternalRoleNotFoundException;
import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.domain.models.InternalRole;
import dev.nipponten.domain.models.PageResult;
import dev.nipponten.domain.repositories.InternalRepository;
import dev.nipponten.domain.repositories.InternalRoleRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class InternalRoleService {

    @Inject InternalRoleRepository repository;

    @Inject InternalRepository internalRepository;

    @Inject AuthenticationCache authenticationCache;

    @Inject CurrentUser currentUser;

    // Criar, editar ou reativar uma role exige ter todas as permissões que ela terá: senão quem
    // gerencia roles daria a si mesmo, ou a outros, o que não tem.
    public InternalRole create(InternalRole model) {
        currentUser.requireHolds(model.permissions());
        requireUniqueName(model.name(), null);
        if (model.defaultRole()) {
            clearCurrentDefault(null);
        }
        return repository.save(
                new InternalRole(
                        null, model.name(), model.permissions(), model.defaultRole(), true, false));
    }

    public InternalRole getById(Long id) {
        InternalRole model = repository.getById(id);
        if (model == null) throw new InternalRoleNotFoundException(id);
        return model;
    }

    public List<InternalRole> getAll() {
        return repository.getAll();
    }

    public PageResult<InternalRole> getPage(int page, int size) {
        return repository.getPage(page, size);
    }

    public InternalRole getDefault() {
        InternalRole model = repository.getDefault();
        if (model == null) {
            throw new InvalidRequestException("No default internal role configured");
        }
        return model;
    }

    public InternalRole update(Long id, InternalRole model) {
        InternalRole current = getById(id);
        requireNotReserved(current);
        currentUser.requireHolds(model.permissions());
        requireUniqueName(model.name(), id);
        if (model.defaultRole()) {
            clearCurrentDefault(id);
        }
        authenticationCache.invalidateAll();
        return repository.save(
                new InternalRole(
                        id,
                        model.name(),
                        model.permissions(),
                        model.defaultRole(),
                        current.active(),
                        false));
    }

    public InternalRole setActive(Long id, boolean active) {
        InternalRole current = getById(id);
        requireNotReserved(current);
        if (active) {
            currentUser.requireHolds(current.permissions());
        }
        authenticationCache.invalidateAll();
        return repository.save(
                new InternalRole(
                        id,
                        current.name(),
                        current.permissions(),
                        current.defaultRole(),
                        active,
                        false));
    }

    public void delete(Long id) {
        InternalRole model = getById(id);
        requireNotReserved(model);
        int internals = internalRepository.getByInternalRole(id).size();
        if (internals > 0) {
            throw new InvalidRequestException(
                    "Internal role "
                            + id
                            + " cannot be deleted: assigned to "
                            + internals
                            + " internal user(s)");
        }
        repository.remove(model);
    }

    // A role do super_admin é mantida pelo SuperAdminBootstrap: as permissões dela acompanham o
    // enum.
    private static void requireNotReserved(InternalRole role) {
        if (role.reserved()) {
            throw new InvalidRequestException(
                    "Internal role " + role.name() + " is reserved and cannot be changed");
        }
    }

    private void requireUniqueName(String name, Long roleId) {
        InternalRole existing = repository.getByName(name);
        if (existing != null && !existing.id().equals(roleId)) {
            throw new InvalidRequestException("Internal role name already in use: " + name);
        }
    }

    private void clearCurrentDefault(Long roleId) {
        InternalRole currentDefault = repository.getDefault();
        if (currentDefault != null && !currentDefault.id().equals(roleId)) {
            repository.save(
                    new InternalRole(
                            currentDefault.id(),
                            currentDefault.name(),
                            currentDefault.permissions(),
                            false,
                            currentDefault.active(),
                            currentDefault.reserved()));
        }
    }
}

package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.InternalRoleNotFoundException;
import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.domain.models.InternalRole;
import dev.nipponten.domain.repositories.InternalRepository;
import dev.nipponten.domain.repositories.InternalRoleRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class InternalRoleService {

    @Inject InternalRoleRepository repository;

    @Inject InternalRepository internalRepository;

    public InternalRole create(InternalRole model) {
        return repository.save(model);
    }

    public InternalRole getById(Long id) {
        InternalRole model = repository.getById(id);
        if (model == null) throw new InternalRoleNotFoundException(id);
        return model;
    }

    public List<InternalRole> getAll() {
        return repository.getAll();
    }

    public InternalRole update(Long id, InternalRole model) {
        getById(id);
        return repository.save(model);
    }

    public void delete(Long id) {
        InternalRole model = getById(id);
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
}

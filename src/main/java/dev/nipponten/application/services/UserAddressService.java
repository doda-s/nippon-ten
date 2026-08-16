package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.UserAddressNotFoundException;
import dev.nipponten.domain.models.UserAddress;
import dev.nipponten.infrastructure.repositories.UserAddressRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class UserAddressService {

    @Inject UserAddressRepository repository;

    public UserAddress create(UserAddress model) {
        return repository.save(model);
    }

    public UserAddress getById(Long id) {
        UserAddress model = repository.getById(id);
        if (model == null) throw new UserAddressNotFoundException(id);
        return model;
    }

    public List<UserAddress> getAll() {
        return repository.getAll();
    }

    public UserAddress update(Long id, UserAddress model) {
        getById(id);
        return repository.save(model);
    }

    public void delete(Long id) {
        UserAddress model = getById(id);
        repository.remove(model);
    }

    public List<UserAddress> getByClient(Long clientId) {
        return repository.getByClient(clientId);
    }
}

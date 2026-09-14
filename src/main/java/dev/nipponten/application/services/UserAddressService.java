package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.UserAddressNotFoundException;
import dev.nipponten.domain.models.UserAddress;
import dev.nipponten.domain.repositories.UserAddressRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class UserAddressService {

    @Inject UserAddressRepository repository;

    @Inject ClientService clientService;

    public UserAddress create(UserAddress model) {
        clientService.getById(model.clientId());
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

    public UserAddress update(Long clientId, Long id, UserAddress model) {
        requireByClient(clientId, id);
        return repository.save(model);
    }

    public void delete(Long clientId, Long id) {
        UserAddress model = requireByClient(clientId, id);
        repository.remove(model);
    }

    public List<UserAddress> getByClient(Long clientId) {
        clientService.getById(clientId);
        return repository.getByClient(clientId);
    }

    public UserAddress requireByClient(Long clientId, Long id) {
        clientService.getById(clientId);
        UserAddress model = getById(id);
        if (!clientId.equals(model.clientId())) {
            throw new UserAddressNotFoundException(id);
        }
        return model;
    }
}

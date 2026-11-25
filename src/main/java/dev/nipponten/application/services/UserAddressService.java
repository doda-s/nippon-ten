package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.UserAddressNotFoundException;
import dev.nipponten.domain.models.Address;
import dev.nipponten.domain.models.UserAddress;
import dev.nipponten.domain.repositories.UserAddressRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class UserAddressService {

    @Inject UserAddressRepository repository;

    @Inject ClientService clientService;

    @Inject CepService cepService;

    public UserAddress create(UserAddress model) {
        clientService.getById(model.clientId());
        return repository.save(withCep(model));
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
        return repository.save(withCep(model));
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

    // Bairro, cidade e UF vêm do CEP, não do corpo: são eles que vão para a geocodificação da
    // entrega.
    private UserAddress withCep(UserAddress model) {
        Address address = cepService.complete(model.toAddress());
        return new UserAddress(
                model.id(),
                model.clientId(),
                model.streetAddress(),
                model.number(),
                model.cep(),
                model.complement(),
                address.neighborhood(),
                address.city(),
                address.state());
    }
}

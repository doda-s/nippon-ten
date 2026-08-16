package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.ClientNotFoundException;
import dev.nipponten.domain.models.Client;
import dev.nipponten.infrastructure.repositories.ClientRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class ClientService {

    @Inject
    ClientRepository repository;

    public Client create(Client model) {
        return repository.save(model);
    }

    public Client getById(Long id) {
        Client model = repository.getById(id);
        if (model == null) throw new ClientNotFoundException(id);
        return model;
    }

    public List<Client> getAll() {
        return repository.getAll();
    }

    public Client update(Long id, Client model) {
        getById(id);
        return repository.save(model);
    }

    public void delete(Long id) {
        Client model = getById(id);
        repository.remove(model);
    }

    public List<Client> getByUser(Long userId) {
        return repository.getByUser(userId);
    }
}

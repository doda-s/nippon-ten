package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.Client;
import java.util.List;

public interface ClientRepository {
    Client save(Client model);

    void remove(Client model);

    Client getById(Long id);

    List<Client> getAll();

    java.util.List<Client> getByUser(Long userId);
}

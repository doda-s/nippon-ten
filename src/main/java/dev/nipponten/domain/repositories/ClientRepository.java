package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.Client;
import java.util.List;

public interface ClientRepository {
    Client save(Client model);

    void remove(Client model);

    Client getById(Long id);

    // Trava a linha até o fim da transação: dois checkouts simultâneos não gastam o mesmo ponto.
    Client lockById(Long id);

    List<Client> getAll();

    java.util.List<Client> getByUser(Long userId);
}

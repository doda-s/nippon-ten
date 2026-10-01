package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.ClientNotFoundException;
import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.domain.models.Client;
import dev.nipponten.domain.repositories.ClientRepository;
import dev.nipponten.domain.repositories.InternalRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ClientService {

    @Inject ClientRepository repository;

    // Repositório, e não o service, para não criar ciclo ClientService <-> InternalService.
    @Inject InternalRepository internalRepository;

    public Client create(Client model) {
        requireNotInternal(model.userId());
        return repository.save(withPromotionPoints(model, 0));
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
        Client current = getById(id);
        return repository.save(withPromotionPoints(model, current.promotionPoints()));
    }

    public void delete(Long id) {
        Client model = getById(id);
        repository.remove(model);
    }

    public Client getByUser(Long userId) {
        return findByUser(userId).orElseThrow(() -> ClientNotFoundException.forUser(userId));
    }

    public Optional<Client> findByUser(Long userId) {
        return repository.getByUser(userId).stream().findFirst();
    }

    // Um User é sempre ou cliente ou interno, nunca os dois.
    private void requireNotInternal(Long userId) {
        if (!internalRepository.getByUser(userId).isEmpty()) {
            throw new InvalidRequestException(
                    "User " + userId + " is already an internal user and cannot become a client");
        }
    }

    // Pontos nunca vêm do request: começam em 0 e são preservados em updates cadastrais.
    private Client withPromotionPoints(Client model, Integer promotionPoints) {
        return new Client(
                model.id(),
                model.userId(),
                model.name(),
                model.lastName(),
                model.cpf(),
                promotionPoints);
    }
}

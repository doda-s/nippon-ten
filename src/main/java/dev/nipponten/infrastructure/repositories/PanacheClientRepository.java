package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.Client;
import dev.nipponten.infrastructure.entities.ClientEntity;
import dev.nipponten.infrastructure.mappers.ClientMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class PanacheClientRepository implements ClientRepository, PanacheRepository<ClientEntity> {

    @Inject
    ClientMapper mapper;

    @Override
    @Transactional
    public Client save(Client model) {
        ClientEntity entity;
        if (model.id() == null) {
            entity = mapper.toEntity(model);
            persist(entity);
        } else {
            entity = findById(model.id());
            mapper.updateEntity(entity, model);
        }
        return mapper.toModel(entity);
    }

    @Override
    @Transactional
    public void remove(Client model) {
        delete(findById(model.id()));
    }

    @Override
    public Client getById(Long id) {
        ClientEntity entity = findById(id);
        return entity == null ? null : mapper.toModel(entity);
    }

    @Override
    public List<Client> getAll() {
        return listAll().stream().map(mapper::toModel).toList();
    }

    @Override
    public List<Client> getByUser(Long userId) {
        return list("user.id", userId).stream().map(mapper::toModel).toList();
    }
}

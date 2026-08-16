package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.UserAddress;
import dev.nipponten.infrastructure.entities.UserAddressEntity;
import dev.nipponten.infrastructure.mappers.UserAddressMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class PanacheUserAddressRepository
        implements UserAddressRepository, PanacheRepository<UserAddressEntity> {

    @Inject UserAddressMapper mapper;

    @Override
    @Transactional
    public UserAddress save(UserAddress model) {
        UserAddressEntity entity;
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
    public void remove(UserAddress model) {
        delete(findById(model.id()));
    }

    @Override
    public UserAddress getById(Long id) {
        UserAddressEntity entity = findById(id);
        return entity == null ? null : mapper.toModel(entity);
    }

    @Override
    public List<UserAddress> getAll() {
        return listAll().stream().map(mapper::toModel).toList();
    }

    @Override
    public List<UserAddress> getByClient(Long clientId) {
        return list("client.id", clientId).stream().map(mapper::toModel).toList();
    }
}

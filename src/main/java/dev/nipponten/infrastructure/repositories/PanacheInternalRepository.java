package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.Internal;
import dev.nipponten.infrastructure.entities.InternalEntity;
import dev.nipponten.infrastructure.mappers.InternalMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class PanacheInternalRepository
        implements InternalRepository, PanacheRepository<InternalEntity> {

    @Inject InternalMapper mapper;

    @Override
    @Transactional
    public Internal save(Internal model) {
        InternalEntity entity;
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
    public void remove(Internal model) {
        delete(findById(model.id()));
    }

    @Override
    public Internal getById(Long id) {
        InternalEntity entity = findById(id);
        return entity == null ? null : mapper.toModel(entity);
    }

    @Override
    public List<Internal> getAll() {
        return listAll().stream().map(mapper::toModel).toList();
    }

    @Override
    public List<Internal> getByUser(Long userId) {
        return list("user.id", userId).stream().map(mapper::toModel).toList();
    }

    @Override
    public List<Internal> getByInternalRole(Long internalRoleId) {
        return list("internalRole.id", internalRoleId).stream().map(mapper::toModel).toList();
    }
}

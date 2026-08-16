package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.Combo;
import dev.nipponten.infrastructure.entities.ComboEntity;
import dev.nipponten.infrastructure.mappers.ComboMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class PanacheComboRepository implements ComboRepository, PanacheRepository<ComboEntity> {

    @Inject ComboMapper mapper;

    @Override
    @Transactional
    public Combo save(Combo model) {
        ComboEntity entity;
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
    public void remove(Combo model) {
        delete(findById(model.id()));
    }

    @Override
    public Combo getById(Long id) {
        ComboEntity entity = findById(id);
        return entity == null ? null : mapper.toModel(entity);
    }

    @Override
    public List<Combo> getAll() {
        return listAll().stream().map(mapper::toModel).toList();
    }
}

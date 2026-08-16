package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.Ingredient;
import dev.nipponten.infrastructure.entities.IngredientEntity;
import dev.nipponten.infrastructure.mappers.IngredientMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class PanacheIngredientRepository implements IngredientRepository, PanacheRepository<IngredientEntity> {

    @Inject
    IngredientMapper mapper;

    @Override
    @Transactional
    public Ingredient save(Ingredient model) {
        IngredientEntity entity;
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
    public void remove(Ingredient model) {
        delete(findById(model.id()));
    }

    @Override
    public Ingredient getById(Long id) {
        IngredientEntity entity = findById(id);
        return entity == null ? null : mapper.toModel(entity);
    }

    @Override
    public List<Ingredient> getAll() {
        return listAll().stream().map(mapper::toModel).toList();
    }
}

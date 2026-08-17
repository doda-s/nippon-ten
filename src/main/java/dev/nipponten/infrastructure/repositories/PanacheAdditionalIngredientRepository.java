package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.AdditionalIngredient;
import dev.nipponten.infrastructure.entities.AdditionalIngredientEntity;
import dev.nipponten.infrastructure.mappers.AdditionalIngredientMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class PanacheAdditionalIngredientRepository
        implements AdditionalIngredientRepository, PanacheRepository<AdditionalIngredientEntity> {

    @Inject AdditionalIngredientMapper mapper;

    @Override
    @Transactional
    public AdditionalIngredient save(AdditionalIngredient model) {
        AdditionalIngredientEntity entity;
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
    public void remove(AdditionalIngredient model) {
        delete(findById(model.id()));
    }

    @Override
    public AdditionalIngredient getById(Long id) {
        AdditionalIngredientEntity entity = findById(id);
        return entity == null ? null : mapper.toModel(entity);
    }

    @Override
    public List<AdditionalIngredient> getAll() {
        return listAll().stream().map(mapper::toModel).toList();
    }

    @Override
    public List<AdditionalIngredient> getByProduct(Long productId) {
        return list("product.id", productId).stream().map(mapper::toModel).toList();
    }

    @Override
    public List<AdditionalIngredient> getByIngredient(Long ingredientId) {
        return list("ingredient.id", ingredientId).stream().map(mapper::toModel).toList();
    }
}

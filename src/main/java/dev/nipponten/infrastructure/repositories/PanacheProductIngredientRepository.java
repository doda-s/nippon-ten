package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.ProductIngredient;
import dev.nipponten.infrastructure.entities.ProductIngredientEntity;
import dev.nipponten.infrastructure.mappers.ProductIngredientMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class PanacheProductIngredientRepository implements ProductIngredientRepository, PanacheRepository<ProductIngredientEntity> {

    @Inject
    ProductIngredientMapper mapper;

    @Override
    @Transactional
    public ProductIngredient save(ProductIngredient model) {
        ProductIngredientEntity entity;
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
    public void remove(ProductIngredient model) {
        delete(findById(model.id()));
    }

    @Override
    public ProductIngredient getById(Long id) {
        ProductIngredientEntity entity = findById(id);
        return entity == null ? null : mapper.toModel(entity);
    }

    @Override
    public List<ProductIngredient> getAll() {
        return listAll().stream().map(mapper::toModel).toList();
    }

    @Override
    public List<ProductIngredient> getByProduct(Long productId) {
        return list("product.id", productId).stream().map(mapper::toModel).toList();
    }

    @Override
    public List<ProductIngredient> getByIngredient(Long ingredientId) {
        return list("ingredient.id", ingredientId).stream().map(mapper::toModel).toList();
    }
}

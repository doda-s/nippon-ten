package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.Product;
import dev.nipponten.infrastructure.entities.ProductEntity;
import dev.nipponten.infrastructure.mappers.ProductMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class PanacheProductRepository
        implements ProductRepository, PanacheRepository<ProductEntity> {

    @Inject ProductMapper mapper;

    @Override
    @Transactional
    public Product save(Product model) {
        ProductEntity entity;
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
    public void remove(Product model) {
        delete(findById(model.id()));
    }

    @Override
    public Product getById(Long id) {
        ProductEntity entity = findById(id);
        return entity == null ? null : mapper.toModel(entity);
    }

    @Override
    public List<Product> getAll() {
        return listAll().stream().map(mapper::toModel).toList();
    }
}

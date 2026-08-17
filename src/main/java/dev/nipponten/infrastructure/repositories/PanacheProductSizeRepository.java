package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.ProductSize;
import dev.nipponten.infrastructure.entities.ProductSizeEntity;
import dev.nipponten.infrastructure.mappers.ProductSizeMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class PanacheProductSizeRepository
        implements ProductSizeRepository, PanacheRepository<ProductSizeEntity> {

    @Inject ProductSizeMapper mapper;

    @Override
    @Transactional
    public ProductSize save(ProductSize model) {
        ProductSizeEntity entity;
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
    public void remove(ProductSize model) {
        delete(findById(model.id()));
    }

    @Override
    public ProductSize getById(Long id) {
        ProductSizeEntity entity = findById(id);
        return entity == null ? null : mapper.toModel(entity);
    }

    @Override
    public List<ProductSize> getAll() {
        return listAll().stream().map(mapper::toModel).toList();
    }

    @Override
    public List<ProductSize> getByProduct(Long productId) {
        return list("product.id", productId).stream().map(mapper::toModel).toList();
    }
}

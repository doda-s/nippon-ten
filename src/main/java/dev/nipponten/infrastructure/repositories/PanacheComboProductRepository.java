package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.ComboProduct;
import dev.nipponten.infrastructure.entities.ComboProductEntity;
import dev.nipponten.infrastructure.mappers.ComboProductMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class PanacheComboProductRepository implements ComboProductRepository, PanacheRepository<ComboProductEntity> {

    @Inject
    ComboProductMapper mapper;

    @Override
    @Transactional
    public ComboProduct save(ComboProduct model) {
        ComboProductEntity entity;
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
    public void remove(ComboProduct model) {
        delete(findById(model.id()));
    }

    @Override
    public ComboProduct getById(Long id) {
        ComboProductEntity entity = findById(id);
        return entity == null ? null : mapper.toModel(entity);
    }

    @Override
    public List<ComboProduct> getAll() {
        return listAll().stream().map(mapper::toModel).toList();
    }

    @Override
    public List<ComboProduct> getByCombo(Long comboId) {
        return list("combo.id", comboId).stream().map(mapper::toModel).toList();
    }

    @Override
    public List<ComboProduct> getByProduct(Long productId) {
        return list("product.id", productId).stream().map(mapper::toModel).toList();
    }
}

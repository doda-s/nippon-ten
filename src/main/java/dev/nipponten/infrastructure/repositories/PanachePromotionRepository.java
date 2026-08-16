package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.Promotion;
import dev.nipponten.infrastructure.entities.PromotionEntity;
import dev.nipponten.infrastructure.mappers.PromotionMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class PanachePromotionRepository implements PromotionRepository, PanacheRepository<PromotionEntity> {

    @Inject
    PromotionMapper mapper;

    @Override
    @Transactional
    public Promotion save(Promotion model) {
        PromotionEntity entity;
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
    public void remove(Promotion model) {
        delete(findById(model.id()));
    }

    @Override
    public Promotion getById(Long id) {
        PromotionEntity entity = findById(id);
        return entity == null ? null : mapper.toModel(entity);
    }

    @Override
    public List<Promotion> getAll() {
        return listAll().stream().map(mapper::toModel).toList();
    }

    @Override
    public List<Promotion> getByProduct(Long productId) {
        return list("product.id", productId).stream().map(mapper::toModel).toList();
    }

    @Override
    public List<Promotion> getByPromotionType(Long promotionTypeId) {
        return list("promotionType.id", promotionTypeId).stream().map(mapper::toModel).toList();
    }
}

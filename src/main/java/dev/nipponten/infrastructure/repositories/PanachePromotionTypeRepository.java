package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.PromotionType;
import dev.nipponten.infrastructure.entities.PromotionTypeEntity;
import dev.nipponten.infrastructure.mappers.PromotionTypeMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class PanachePromotionTypeRepository
        implements PromotionTypeRepository, PanacheRepository<PromotionTypeEntity> {

    @Inject PromotionTypeMapper mapper;

    @Override
    @Transactional
    public PromotionType save(PromotionType model) {
        PromotionTypeEntity entity;
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
    public void remove(PromotionType model) {
        delete(findById(model.id()));
    }

    @Override
    public PromotionType getById(Long id) {
        PromotionTypeEntity entity = findById(id);
        return entity == null ? null : mapper.toModel(entity);
    }

    @Override
    public List<PromotionType> getAll() {
        return listAll().stream().map(mapper::toModel).toList();
    }
}

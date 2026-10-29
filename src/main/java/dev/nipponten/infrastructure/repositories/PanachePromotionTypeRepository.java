package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.PageResult;
import dev.nipponten.domain.models.PromotionType;
import dev.nipponten.domain.repositories.PromotionTypeRepository;
import dev.nipponten.infrastructure.entities.PromotionTypeEntity;
import dev.nipponten.infrastructure.mappers.PromotionTypeMapper;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
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

    @Override
    public PageResult<PromotionType> getPage(int page, int size) {
        PanacheQuery<PromotionTypeEntity> query = findAll(Sort.by("id"));
        long total = query.count();
        List<PromotionType> content =
                query.page(Page.of(page, size)).list().stream().map(mapper::toModel).toList();
        return new PageResult<>(content, total);
    }
}

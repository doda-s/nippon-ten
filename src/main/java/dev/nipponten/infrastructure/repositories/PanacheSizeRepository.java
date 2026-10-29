package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.PageResult;
import dev.nipponten.domain.models.Size;
import dev.nipponten.domain.repositories.SizeRepository;
import dev.nipponten.infrastructure.entities.SizeEntity;
import dev.nipponten.infrastructure.mappers.SizeMapper;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class PanacheSizeRepository implements SizeRepository, PanacheRepository<SizeEntity> {

    @Inject SizeMapper mapper;

    @Override
    @Transactional
    public Size save(Size model) {
        SizeEntity entity;
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
    public void remove(Size model) {
        delete(findById(model.id()));
    }

    @Override
    public Size getById(Long id) {
        SizeEntity entity = findById(id);
        return entity == null ? null : mapper.toModel(entity);
    }

    @Override
    public List<Size> getAll() {
        return listAll().stream().map(mapper::toModel).toList();
    }

    @Override
    public PageResult<Size> getPage(int page, int size) {
        PanacheQuery<SizeEntity> query = findAll(Sort.by("id"));
        long total = query.count();
        List<Size> content =
                query.page(Page.of(page, size)).list().stream().map(mapper::toModel).toList();
        return new PageResult<>(content, total);
    }
}

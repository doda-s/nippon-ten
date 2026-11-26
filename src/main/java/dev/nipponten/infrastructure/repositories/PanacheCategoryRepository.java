package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.Category;
import dev.nipponten.domain.models.PageResult;
import dev.nipponten.domain.repositories.CategoryRepository;
import dev.nipponten.infrastructure.entities.CategoryEntity;
import dev.nipponten.infrastructure.mappers.CategoryMapper;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class PanacheCategoryRepository
        implements CategoryRepository, PanacheRepository<CategoryEntity> {

    @Inject CategoryMapper mapper;

    @Override
    @Transactional
    public Category save(Category model) {
        CategoryEntity entity;
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
    public void remove(Category model) {
        delete(findById(model.id()));
    }

    @Override
    public Category getById(Long id) {
        CategoryEntity entity = findById(id);
        return entity == null ? null : mapper.toModel(entity);
    }

    @Override
    public Category getByName(String name) {
        return find("lower(name) = lower(?1)", name.trim())
                .firstResultOptional()
                .map(mapper::toModel)
                .orElse(null);
    }

    @Override
    public PageResult<Category> getPage(int page, int size) {
        PanacheQuery<CategoryEntity> query = findAll(Sort.by("displayOrder").and("id"));
        long total = query.count();
        List<Category> content =
                query.page(Page.of(page, size)).list().stream().map(mapper::toModel).toList();
        return new PageResult<>(content, total);
    }
}

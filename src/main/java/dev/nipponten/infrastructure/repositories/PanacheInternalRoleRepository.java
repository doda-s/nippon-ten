package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.InternalRole;
import dev.nipponten.domain.models.PageResult;
import dev.nipponten.domain.repositories.InternalRoleRepository;
import dev.nipponten.infrastructure.entities.InternalRoleEntity;
import dev.nipponten.infrastructure.mappers.InternalRoleMapper;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class PanacheInternalRoleRepository
        implements InternalRoleRepository, PanacheRepository<InternalRoleEntity> {

    @Inject InternalRoleMapper mapper;

    @Override
    @Transactional
    public InternalRole save(InternalRole model) {
        InternalRoleEntity entity;
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
    public void remove(InternalRole model) {
        delete(findById(model.id()));
    }

    @Override
    public InternalRole getById(Long id) {
        InternalRoleEntity entity = findById(id);
        return entity == null ? null : mapper.toModel(entity);
    }

    @Override
    public List<InternalRole> getAll() {
        return listAll().stream().map(mapper::toModel).toList();
    }

    @Override
    public PageResult<InternalRole> getPage(int page, int size) {
        PanacheQuery<InternalRoleEntity> query = findAll(Sort.by("id"));
        long total = query.count();
        List<InternalRole> content =
                query.page(Page.of(page, size)).list().stream().map(mapper::toModel).toList();
        return new PageResult<>(content, total);
    }

    @Override
    public InternalRole getByName(String name) {
        InternalRoleEntity entity = find("name", name).firstResult();
        return entity == null ? null : mapper.toModel(entity);
    }

    @Override
    public InternalRole getDefault() {
        InternalRoleEntity entity = find("defaultRole", true).firstResult();
        return entity == null ? null : mapper.toModel(entity);
    }

    @Override
    public InternalRole getReserved() {
        InternalRoleEntity entity = find("reserved", true).firstResult();
        return entity == null ? null : mapper.toModel(entity);
    }
}

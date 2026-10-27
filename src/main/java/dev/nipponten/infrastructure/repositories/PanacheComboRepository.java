package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.Combo;
import dev.nipponten.domain.models.PageResult;
import dev.nipponten.domain.repositories.ComboRepository;
import dev.nipponten.infrastructure.entities.ComboEntity;
import dev.nipponten.infrastructure.mappers.ComboMapper;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class PanacheComboRepository implements ComboRepository, PanacheRepository<ComboEntity> {

    @Inject ComboMapper mapper;

    @Override
    @Transactional
    public Combo save(Combo model) {
        ComboEntity entity;
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
    public void remove(Combo model) {
        delete(findById(model.id()));
    }

    @Override
    public Combo getById(Long id) {
        ComboEntity entity = findById(id);
        return entity == null ? null : mapper.toModel(entity);
    }

    @Override
    public List<Combo> getAll() {
        return listAll().stream().map(mapper::toModel).toList();
    }

    @Override
    public List<Combo> getAvailable(LocalDateTime now) {
        return list(
                        "status = ?1 and (startDate is null or startDate <= ?2)"
                                + " and (endDate is null or endDate > ?2)",
                        ComboEntity.Status.ACTIVE,
                        now)
                .stream()
                .map(mapper::toModel)
                .toList();
    }

    @Override
    public PageResult<Combo> getPage(int page, int size) {
        PanacheQuery<ComboEntity> query = findAll(Sort.by("id"));
        long total = query.count();
        List<Combo> content =
                query.page(Page.of(page, size)).list().stream().map(mapper::toModel).toList();
        return new PageResult<>(content, total);
    }

    @Override
    public PageResult<Combo> getAvailablePage(LocalDateTime now, int page, int size) {
        PanacheQuery<ComboEntity> query =
                find(
                        "status = ?1 and (startDate is null or startDate <= ?2)"
                                + " and (endDate is null or endDate > ?2)",
                        Sort.by("id"),
                        ComboEntity.Status.ACTIVE,
                        now);
        long total = query.count();
        List<Combo> content =
                query.page(Page.of(page, size)).list().stream().map(mapper::toModel).toList();
        return new PageResult<>(content, total);
    }
}

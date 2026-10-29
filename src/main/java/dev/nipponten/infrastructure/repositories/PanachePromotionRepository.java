package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.PageResult;
import dev.nipponten.domain.models.Promotion;
import dev.nipponten.domain.repositories.PromotionRepository;
import dev.nipponten.infrastructure.entities.PromotionEntity;
import dev.nipponten.infrastructure.mappers.PromotionMapper;
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
public class PanachePromotionRepository
        implements PromotionRepository, PanacheRepository<PromotionEntity> {

    @Inject PromotionMapper mapper;

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
    public List<Promotion> getAvailable(LocalDateTime now) {
        return list(
                        "status = ?1 and startDate <= ?2 and endDate > ?2",
                        PromotionEntity.Status.ACTIVE,
                        now)
                .stream()
                .map(mapper::toModel)
                .toList();
    }

    @Override
    public List<Promotion> getByProduct(Long productId) {
        return list("select p from PromotionEntity p join p.products i where i.id = ?1", productId)
                .stream()
                .map(mapper::toModel)
                .toList();
    }

    @Override
    public List<Promotion> getByCombo(Long comboId) {
        return list("select p from PromotionEntity p join p.combos i where i.id = ?1", comboId)
                .stream()
                .map(mapper::toModel)
                .toList();
    }

    @Override
    public List<Promotion> getOverlappingByProduct(
            Long productId, LocalDateTime start, LocalDateTime end) {
        return list(
                        "select p from PromotionEntity p join p.products i"
                                + " where i.id = ?1 and p.startDate < ?3 and p.endDate > ?2",
                        productId,
                        start,
                        end)
                .stream()
                .map(mapper::toModel)
                .toList();
    }

    @Override
    public List<Promotion> getOverlappingByCombo(
            Long comboId, LocalDateTime start, LocalDateTime end) {
        return list(
                        "select p from PromotionEntity p join p.combos i"
                                + " where i.id = ?1 and p.startDate < ?3 and p.endDate > ?2",
                        comboId,
                        start,
                        end)
                .stream()
                .map(mapper::toModel)
                .toList();
    }

    @Override
    public List<Promotion> getByPromotionType(Long promotionTypeId) {
        return list("promotionType.id", promotionTypeId).stream().map(mapper::toModel).toList();
    }

    @Override
    public PageResult<Promotion> getPage(int page, int size) {
        PanacheQuery<PromotionEntity> query = findAll(Sort.by("id"));
        long total = query.count();
        List<Promotion> content =
                query.page(Page.of(page, size)).list().stream().map(mapper::toModel).toList();
        return new PageResult<>(content, total);
    }

    @Override
    public PageResult<Promotion> getAvailablePage(LocalDateTime now, int page, int size) {
        PanacheQuery<PromotionEntity> query =
                find(
                        "status = ?1 and startDate <= ?2 and endDate > ?2",
                        Sort.by("id"),
                        PromotionEntity.Status.ACTIVE,
                        now);
        long total = query.count();
        List<Promotion> content =
                query.page(Page.of(page, size)).list().stream().map(mapper::toModel).toList();
        return new PageResult<>(content, total);
    }
}

package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.Order;
import dev.nipponten.domain.models.PageResult;
import dev.nipponten.domain.repositories.OrderRepository;
import dev.nipponten.infrastructure.entities.OrderEntity;
import dev.nipponten.infrastructure.mappers.OrderMapper;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class PanacheOrderRepository implements OrderRepository, PanacheRepository<OrderEntity> {

    @Inject OrderMapper mapper;

    @Override
    @Transactional
    public Order save(Order model) {
        OrderEntity entity;
        if (model.id() == null) {
            entity = mapper.toEntity(model);
            persist(entity);
        } else {
            entity = findById(model.id());
            mapper.updateEntity(entity, model);
        }
        // Gera os ids dos itens antes de devolver o modelo.
        flush();
        return mapper.toModel(entity);
    }

    @Override
    public Order getById(Long id) {
        OrderEntity entity = findById(id);
        return entity == null ? null : mapper.toModel(entity);
    }

    @Override
    public boolean existsByCode(String code) {
        return count("code", code) > 0;
    }

    @Override
    public Order getByCode(String code) {
        OrderEntity entity = find("code", code).firstResult();
        return entity == null ? null : mapper.toModel(entity);
    }

    @Override
    @Transactional
    public void updateTrackingTokenHash(Long id, String trackingTokenHash) {
        update("trackingTokenHash = ?1 where id = ?2", trackingTokenHash, id);
    }

    // Mais recentes primeiro: é o histórico do cliente. O id desempata e mantém as páginas
    // estáveis.
    @Override
    public PageResult<Order> getPageByClient(Long clientId, int page, int size) {
        PanacheQuery<OrderEntity> query =
                find(
                        "clientId",
                        Sort.by("createdAt").descending().and("id").descending(),
                        clientId);
        long total = query.count();
        List<Order> content =
                query.page(Page.of(page, size)).list().stream().map(mapper::toModel).toList();
        return new PageResult<>(content, total);
    }

    @Override
    public PageResult<Order> getPage(int page, int size) {
        return toPage(findAll(Sort.by("id")), page, size);
    }

    @Override
    public PageResult<Order> getPageByStatus(Order.Status status, int page, int size) {
        return toPage(
                find("status", Sort.by("id"), OrderEntity.Status.valueOf(status.name())),
                page,
                size);
    }

    @Override
    public List<Order> getByStatusCreatedBefore(Order.Status status, Instant cutoff, int limit) {
        return find(
                        "status = ?1 and createdAt < ?2",
                        Sort.by("createdAt").and("id"),
                        OrderEntity.Status.valueOf(status.name()),
                        cutoff)
                .page(Page.of(0, limit))
                .list()
                .stream()
                .map(mapper::toModel)
                .toList();
    }

    private PageResult<Order> toPage(PanacheQuery<OrderEntity> query, int page, int size) {
        long total = query.count();
        List<Order> content =
                query.page(Page.of(page, size)).list().stream().map(mapper::toModel).toList();
        return new PageResult<>(content, total);
    }
}

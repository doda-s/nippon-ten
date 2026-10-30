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
import java.math.BigDecimal;
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
    public List<BigDecimal> getCompletedClientAmountsBetween(Instant from, Instant to) {
        return getEntityManager()
                .createQuery(
                        "select o.total - o.deliveryFee from OrderEntity o"
                                + " where o.clientId is not null and o.status = :status"
                                + " and o.createdAt >= :from and o.createdAt < :to",
                        BigDecimal.class)
                .setParameter("status", OrderEntity.Status.COMPLETED)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    @Override
    public long sumPointsRedeemedBetween(Instant from, Instant to) {
        Long sum =
                getEntityManager()
                        .createQuery(
                                "select sum(o.pointsRedeemed) from OrderEntity o"
                                        + " where o.status <> :cancelled"
                                        + " and o.createdAt >= :from and o.createdAt < :to",
                                Long.class)
                        .setParameter("cancelled", OrderEntity.Status.CANCELLED)
                        .setParameter("from", from)
                        .setParameter("to", to)
                        .getSingleResult();
        return sum == null ? 0 : sum;
    }

    private PageResult<Order> toPage(PanacheQuery<OrderEntity> query, int page, int size) {
        long total = query.count();
        List<Order> content =
                query.page(Page.of(page, size)).list().stream().map(mapper::toModel).toList();
        return new PageResult<>(content, total);
    }
}

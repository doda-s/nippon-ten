package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.PageResult;
import dev.nipponten.domain.models.PointsTransaction;
import dev.nipponten.domain.repositories.PointsTransactionRepository;
import dev.nipponten.infrastructure.entities.PointsTransactionEntity;
import dev.nipponten.infrastructure.mappers.PointsTransactionMapper;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class PanachePointsTransactionRepository
        implements PointsTransactionRepository, PanacheRepository<PointsTransactionEntity> {

    @Inject PointsTransactionMapper mapper;

    @Override
    @Transactional
    public PointsTransaction save(PointsTransaction model) {
        PointsTransactionEntity entity;
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
    public PointsTransaction getById(Long id) {
        PointsTransactionEntity entity = findById(id);
        return entity == null ? null : mapper.toModel(entity);
    }

    // O que vence primeiro é gasto primeiro; lote sem vencimento fica por último.
    @Override
    public List<PointsTransaction> getOpenLots(Long clientId, Instant now) {
        return find(
                        "clientId = :clientId and remaining > 0"
                                + " and (expiresAt is null or expiresAt > :now)",
                        Sort.by(
                                        "expiresAt",
                                        Sort.Direction.Ascending,
                                        Sort.NullPrecedence.NULLS_LAST)
                                .and("id"),
                        Parameters.with("clientId", clientId).and("now", now))
                .list()
                .stream()
                .map(mapper::toModel)
                .toList();
    }

    @Override
    public List<PointsTransaction> getDueLots(Long clientId, Instant now) {
        return find(
                        "clientId = :clientId and remaining > 0 and expiresAt <= :now",
                        Sort.by("id"),
                        Parameters.with("clientId", clientId).and("now", now))
                .list()
                .stream()
                .map(mapper::toModel)
                .toList();
    }

    @Override
    public List<PointsTransaction> getByOrder(Long orderId, PointsTransaction.Type type) {
        return find(
                        "orderId = :orderId and type = :type",
                        Sort.by("id"),
                        Parameters.with("orderId", orderId)
                                .and("type", PointsTransactionEntity.Type.valueOf(type.name())))
                .list()
                .stream()
                .map(mapper::toModel)
                .toList();
    }

    // Mais recentes primeiro, como o histórico de pedidos; o id desempata.
    @Override
    public PageResult<PointsTransaction> getPageByClient(Long clientId, int page, int size) {
        PanacheQuery<PointsTransactionEntity> query =
                find(
                        "clientId",
                        Sort.by("createdAt").descending().and("id").descending(),
                        clientId);
        long total = query.count();
        List<PointsTransaction> content =
                query.page(Page.of(page, size)).list().stream().map(mapper::toModel).toList();
        return new PageResult<>(content, total);
    }

    @Override
    public long sumOpen(Instant now) {
        Long sum =
                getEntityManager()
                        .createQuery(
                                "select sum(t.remaining) from PointsTransactionEntity t"
                                        + " where t.remaining > 0"
                                        + " and (t.expiresAt is null or t.expiresAt > :now)",
                                Long.class)
                        .setParameter("now", now)
                        .getSingleResult();
        return sum == null ? 0 : sum;
    }
}

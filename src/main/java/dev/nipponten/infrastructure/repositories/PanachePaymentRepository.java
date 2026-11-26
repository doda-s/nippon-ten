package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.Payment;
import dev.nipponten.domain.repositories.PaymentRepository;
import dev.nipponten.infrastructure.entities.OrderEntity;
import dev.nipponten.infrastructure.entities.PaymentEntity;
import dev.nipponten.infrastructure.mappers.PaymentMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class PanachePaymentRepository
        implements PaymentRepository, PanacheRepository<PaymentEntity> {

    @Inject PaymentMapper mapper;

    @Override
    @Transactional
    public Payment save(Payment model) {
        PaymentEntity entity;
        if (model.id() == null) {
            entity = mapper.toEntity(model);
            persist(entity);
        } else {
            entity = findById(model.id());
            mapper.updateEntity(entity, model);
        }
        // Os índices únicos parciais (uma tentativa aberta por pedido) falham aqui, não no commit.
        flush();
        return mapper.toModel(entity);
    }

    @Override
    public Payment getByExternalId(String externalId) {
        return find("externalId", externalId)
                .firstResultOptional()
                .map(mapper::toModel)
                .orElse(null);
    }

    @Override
    public List<Payment> getPendingExpiredBefore(Instant cutoff, int limit) {
        return find(
                        "status = ?1 and expiresAt < ?2",
                        Sort.by("expiresAt").and("id"),
                        PaymentEntity.Status.PENDING,
                        cutoff)
                .page(Page.of(0, limit))
                .list()
                .stream()
                .map(mapper::toModel)
                .toList();
    }

    @Override
    public List<Payment> getPendingSentCreatedBefore(
            Instant createdCutoff, Instant expiresCutoff, int limit) {
        return find(
                        "status = ?1 and externalId is not null and createdAt < ?2"
                                + " and (expiresAt is null or expiresAt >= ?3)",
                        Sort.by("createdAt", Sort.Direction.Descending).and("id"),
                        PaymentEntity.Status.PENDING,
                        createdCutoff,
                        expiresCutoff)
                .page(Page.of(0, limit))
                .list()
                .stream()
                .map(mapper::toModel)
                .toList();
    }

    @Override
    public List<Payment> getPendingWithoutExternalIdCreatedBefore(Instant cutoff, int limit) {
        return find(
                        "status = ?1 and externalId is null and createdAt < ?2",
                        Sort.by("createdAt").and("id"),
                        PaymentEntity.Status.PENDING,
                        cutoff)
                .page(Page.of(0, limit))
                .list()
                .stream()
                .map(mapper::toModel)
                .toList();
    }

    @Override
    public List<Payment> getApprovedOfCancelledOrders(int limit) {
        return find(
                        "status = ?1 and (statusDetail is null or statusDetail <> ?3) and orderId"
                                + " in (select o.id from OrderEntity o where o.status = ?2)",
                        Sort.by("approvedAt").and("id"),
                        PaymentEntity.Status.APPROVED,
                        OrderEntity.Status.CANCELLED,
                        Payment.REFUND_REJECTED)
                .page(Page.of(0, limit))
                .list()
                .stream()
                .map(mapper::toModel)
                .toList();
    }

    @Override
    public List<Payment> getByOrder(Long orderId) {
        return find("orderId", Sort.by("id"), orderId).list().stream()
                .map(mapper::toModel)
                .toList();
    }
}

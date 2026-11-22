package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.Payment;
import dev.nipponten.domain.repositories.PaymentRepository;
import dev.nipponten.infrastructure.entities.PaymentEntity;
import dev.nipponten.infrastructure.mappers.PaymentMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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
    public List<Payment> getByOrder(Long orderId) {
        return find("orderId", Sort.by("id"), orderId).list().stream()
                .map(mapper::toModel)
                .toList();
    }
}

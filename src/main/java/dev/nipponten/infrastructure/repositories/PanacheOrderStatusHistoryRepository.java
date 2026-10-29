package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.OrderStatusChange;
import dev.nipponten.domain.repositories.OrderStatusHistoryRepository;
import dev.nipponten.infrastructure.entities.OrderStatusHistoryEntity;
import dev.nipponten.infrastructure.mappers.OrderStatusHistoryMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class PanacheOrderStatusHistoryRepository
        implements OrderStatusHistoryRepository, PanacheRepository<OrderStatusHistoryEntity> {

    @Inject OrderStatusHistoryMapper mapper;

    @Override
    @Transactional
    public OrderStatusChange save(OrderStatusChange model) {
        OrderStatusHistoryEntity entity = mapper.toEntity(model);
        persist(entity);
        return mapper.toModel(entity);
    }

    @Override
    public List<OrderStatusChange> getByOrder(Long orderId) {
        return list("orderId", Sort.by("changedAt").and("id"), orderId).stream()
                .map(mapper::toModel)
                .toList();
    }
}

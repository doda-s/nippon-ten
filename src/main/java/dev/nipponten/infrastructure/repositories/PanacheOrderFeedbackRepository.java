package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.OrderFeedback;
import dev.nipponten.domain.repositories.OrderFeedbackRepository;
import dev.nipponten.infrastructure.entities.OrderFeedbackEntity;
import dev.nipponten.infrastructure.mappers.OrderFeedbackMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PanacheOrderFeedbackRepository
        implements OrderFeedbackRepository, PanacheRepository<OrderFeedbackEntity> {

    @Inject OrderFeedbackMapper mapper;

    @Override
    @Transactional
    public OrderFeedback save(OrderFeedback model) {
        OrderFeedbackEntity entity = mapper.toEntity(model);
        persist(entity);
        return mapper.toModel(entity);
    }

    @Override
    public OrderFeedback getByOrder(Long orderId) {
        return find("orderId", orderId).firstResultOptional().map(mapper::toModel).orElse(null);
    }
}

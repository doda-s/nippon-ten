package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.DeliverySettings;
import dev.nipponten.domain.repositories.DeliverySettingsRepository;
import dev.nipponten.infrastructure.entities.DeliverySettingsEntity;
import dev.nipponten.infrastructure.mappers.DeliverySettingsMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PanacheDeliverySettingsRepository
        implements DeliverySettingsRepository, PanacheRepositoryBase<DeliverySettingsEntity, Long> {

    @Inject DeliverySettingsMapper mapper;

    @Override
    public DeliverySettings get() {
        return mapper.toModel(findById(DeliverySettingsEntity.SINGLETON_ID));
    }

    @Override
    @Transactional
    public DeliverySettings save(DeliverySettings model) {
        DeliverySettingsEntity entity = findById(DeliverySettingsEntity.SINGLETON_ID);
        mapper.updateEntity(entity, model);
        return mapper.toModel(entity);
    }
}

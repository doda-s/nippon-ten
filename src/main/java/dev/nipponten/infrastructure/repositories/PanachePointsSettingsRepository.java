package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.PointsSettings;
import dev.nipponten.domain.repositories.PointsSettingsRepository;
import dev.nipponten.infrastructure.entities.PointsSettingsEntity;
import dev.nipponten.infrastructure.mappers.PointsSettingsMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PanachePointsSettingsRepository
        implements PointsSettingsRepository, PanacheRepositoryBase<PointsSettingsEntity, Long> {

    @Inject PointsSettingsMapper mapper;

    @Override
    public PointsSettings get() {
        return mapper.toModel(findById(PointsSettingsEntity.SINGLETON_ID));
    }

    @Override
    @Transactional
    public PointsSettings save(PointsSettings model) {
        PointsSettingsEntity entity = findById(PointsSettingsEntity.SINGLETON_ID);
        mapper.updateEntity(entity, model);
        return mapper.toModel(entity);
    }
}

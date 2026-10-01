package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.Size;
import dev.nipponten.infrastructure.entities.SizeEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SizeMapper {

    public SizeEntity toEntity(Size model) {
        SizeEntity entity = new SizeEntity();
        applyToEntity(entity, model);
        return entity;
    }

    public void updateEntity(SizeEntity entity, Size model) {
        applyToEntity(entity, model);
    }

    private void applyToEntity(SizeEntity entity, Size model) {
        entity.setName(model.name());
    }

    public Size toModel(SizeEntity entity) {
        return new Size(entity.getId(), entity.getName());
    }
}

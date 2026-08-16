package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.InternalRole;
import dev.nipponten.infrastructure.entities.InternalRoleEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InternalRoleMapper {

    public InternalRoleEntity toEntity(InternalRole model) {
        InternalRoleEntity entity = new InternalRoleEntity();
        applyToEntity(entity, model);
        return entity;
    }

    public void updateEntity(InternalRoleEntity entity, InternalRole model) {
        applyToEntity(entity, model);
    }

    private void applyToEntity(InternalRoleEntity entity, InternalRole model) {
        entity.setName(model.name());
    }

    public InternalRole toModel(InternalRoleEntity entity) {
        return new InternalRole(entity.getId(), entity.getName());
    }
}

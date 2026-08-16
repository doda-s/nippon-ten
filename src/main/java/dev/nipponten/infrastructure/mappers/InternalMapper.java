package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.Internal;
import dev.nipponten.infrastructure.entities.InternalEntity;
import dev.nipponten.infrastructure.entities.InternalRoleEntity;
import dev.nipponten.infrastructure.entities.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class InternalMapper {

    @Inject
    EntityManager entityManager;

    public InternalEntity toEntity(Internal model) {
        InternalEntity entity = new InternalEntity();
        applyToEntity(entity, model);
        return entity;
    }

    public void updateEntity(InternalEntity entity, Internal model) {
        applyToEntity(entity, model);
    }

    private void applyToEntity(InternalEntity entity, Internal model) {
        entity.setUser(entityManager.getReference(UserEntity.class, model.userId()));
        entity.setInternalRole(entityManager.getReference(InternalRoleEntity.class, model.internalRoleId()));
        entity.setName(model.name());
        entity.setLastName(model.lastName());
        entity.setCpf(model.cpf());
    }

    public Internal toModel(InternalEntity entity) {
        return new Internal(
                entity.getId(),
                entity.getUser().getId(),
                entity.getInternalRole().getId(),
                entity.getName(),
                entity.getLastName(),
                entity.getCpf());
    }
}

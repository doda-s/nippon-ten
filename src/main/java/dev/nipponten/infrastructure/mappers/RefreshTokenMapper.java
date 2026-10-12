package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.RefreshToken;
import dev.nipponten.infrastructure.entities.RefreshTokenEntity;
import dev.nipponten.infrastructure.entities.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class RefreshTokenMapper {

    @Inject EntityManager entityManager;

    public RefreshTokenEntity toEntity(RefreshToken model) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        applyToEntity(entity, model);
        return entity;
    }

    public void updateEntity(RefreshTokenEntity entity, RefreshToken model) {
        applyToEntity(entity, model);
    }

    private void applyToEntity(RefreshTokenEntity entity, RefreshToken model) {
        entity.setUser(entityManager.getReference(UserEntity.class, model.userId()));
        entity.setSessionId(model.sessionId());
        entity.setTokenHash(model.tokenHash());
        entity.setExpiresAt(model.expiresAt());
        entity.setCreatedAt(model.createdAt());
        entity.setRevokedAt(model.revokedAt());
        entity.setReplacedBy(
                model.replacedById() == null
                        ? null
                        : entityManager.getReference(
                                RefreshTokenEntity.class, model.replacedById()));
    }

    public RefreshToken toModel(RefreshTokenEntity entity) {
        return new RefreshToken(
                entity.getId(),
                entity.getUser().getId(),
                entity.getSessionId(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getRevokedAt(),
                entity.getReplacedBy() == null ? null : entity.getReplacedBy().getId());
    }
}

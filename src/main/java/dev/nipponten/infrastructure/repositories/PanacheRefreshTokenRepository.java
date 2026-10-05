package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.RefreshToken;
import dev.nipponten.domain.repositories.RefreshTokenRepository;
import dev.nipponten.infrastructure.entities.RefreshTokenEntity;
import dev.nipponten.infrastructure.mappers.RefreshTokenMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

@ApplicationScoped
public class PanacheRefreshTokenRepository
        implements RefreshTokenRepository, PanacheRepository<RefreshTokenEntity> {

    @Inject RefreshTokenMapper mapper;

    @Override
    @Transactional
    public RefreshToken save(RefreshToken model) {
        RefreshTokenEntity entity;
        if (model.id() == null) {
            entity = mapper.toEntity(model);
            persist(entity);
        } else {
            entity = findById(model.id());
            mapper.updateEntity(entity, model);
        }
        return mapper.toModel(entity);
    }

    @Override
    public RefreshToken getByHash(String tokenHash) {
        return find("tokenHash", tokenHash).firstResultOptional().map(mapper::toModel).orElse(null);
    }

    @Override
    @Transactional
    public void revokeAllByUser(Long userId, LocalDateTime revokedAt) {
        update("revokedAt = ?1 where user.id = ?2 and revokedAt is null", revokedAt, userId);
    }

    @Override
    @Transactional
    public void removeAllByUser(Long userId) {
        // A cadeia de substituição aponta para tokens do mesmo usuário; solta antes de apagar.
        update("replacedBy = null where user.id = ?1", userId);
        delete("user.id", userId);
    }
}

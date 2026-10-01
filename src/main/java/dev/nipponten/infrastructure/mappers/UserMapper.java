package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.User;
import dev.nipponten.infrastructure.entities.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class UserMapper {

    public UserEntity toEntity(User model) {
        UserEntity entity = new UserEntity();
        applyToEntity(entity, model);
        entity.setCreatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
        return entity;
    }

    public void updateEntity(UserEntity entity, User model) {
        applyToEntity(entity, model);
    }

    private void applyToEntity(UserEntity entity, User model) {
        entity.setEmail(model.email());
        entity.setPassword(model.password());
        entity.setActive(model.active());
    }

    public User toModel(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getEmail(),
                entity.getPassword(),
                entity.getCreatedAt(),
                entity.isActive());
    }
}

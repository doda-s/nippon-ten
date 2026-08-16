package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.Client;
import dev.nipponten.infrastructure.entities.ClientEntity;
import dev.nipponten.infrastructure.entities.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class ClientMapper {

    @Inject
    EntityManager entityManager;

    public ClientEntity toEntity(Client model) {
        ClientEntity entity = new ClientEntity();
        applyToEntity(entity, model);
        return entity;
    }

    public void updateEntity(ClientEntity entity, Client model) {
        applyToEntity(entity, model);
    }

    private void applyToEntity(ClientEntity entity, Client model) {
        entity.setUser(entityManager.getReference(UserEntity.class, model.userId()));
        entity.setName(model.name());
        entity.setLastName(model.lastName());
        entity.setCpf(model.cpf());
        entity.setPromotionPoints(model.promotionPoints());
    }

    public Client toModel(ClientEntity entity) {
        return new Client(
                entity.getId(),
                entity.getUser().getId(),
                entity.getName(),
                entity.getLastName(),
                entity.getCpf(),
                entity.getPromotionPoints());
    }
}

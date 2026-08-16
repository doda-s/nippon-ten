package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.UserAddress;
import dev.nipponten.infrastructure.entities.ClientEntity;
import dev.nipponten.infrastructure.entities.UserAddressEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class UserAddressMapper {

    @Inject
    EntityManager entityManager;

    public UserAddressEntity toEntity(UserAddress model) {
        UserAddressEntity entity = new UserAddressEntity();
        applyToEntity(entity, model);
        return entity;
    }

    public void updateEntity(UserAddressEntity entity, UserAddress model) {
        applyToEntity(entity, model);
    }

    private void applyToEntity(UserAddressEntity entity, UserAddress model) {
        entity.setClient(entityManager.getReference(ClientEntity.class, model.clientId()));
        entity.setStreetAddress(model.streetAddress());
        entity.setNumber(model.number());
        entity.setCep(model.cep());
        entity.setComplement(model.complement());
    }

    public UserAddress toModel(UserAddressEntity entity) {
        return new UserAddress(
                entity.getId(),
                entity.getClient().getId(),
                entity.getStreetAddress(),
                entity.getNumber(),
                entity.getCep(),
                entity.getComplement());
    }
}

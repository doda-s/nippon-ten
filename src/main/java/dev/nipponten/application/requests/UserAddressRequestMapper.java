package dev.nipponten.application.requests;

import dev.nipponten.domain.models.UserAddress;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserAddressRequestMapper {

    public UserAddress toModel(Long id, Long clientId, UserAddressRequest request) {
        return new UserAddress(
                id,
                clientId,
                request.streetAddress(),
                request.number(),
                request.cep(),
                request.complement());
    }
}

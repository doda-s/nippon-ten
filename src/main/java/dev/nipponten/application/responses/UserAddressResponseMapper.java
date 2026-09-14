package dev.nipponten.application.responses;

import dev.nipponten.domain.models.UserAddress;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserAddressResponseMapper {

    public UserAddressResponse toResponse(UserAddress userAddress) {
        return new UserAddressResponse(
                userAddress.id(),
                userAddress.clientId(),
                userAddress.streetAddress(),
                userAddress.number(),
                userAddress.cep(),
                userAddress.complement());
    }
}

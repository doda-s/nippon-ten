package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Client;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClientRequestMapper {

    public Client toModel(Long id, Long userId, ClientRequest request) {
        return new Client(
                id,
                userId,
                request.name(),
                request.lastName(),
                request.cpf(),
                request.promotionPoints());
    }
}

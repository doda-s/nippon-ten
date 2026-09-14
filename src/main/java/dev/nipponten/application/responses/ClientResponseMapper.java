package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Client;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClientResponseMapper {

    public ClientResponse toResponse(Client client) {
        return new ClientResponse(
                client.id(),
                client.userId(),
                client.name(),
                client.lastName(),
                client.cpf(),
                client.promotionPoints());
    }
}

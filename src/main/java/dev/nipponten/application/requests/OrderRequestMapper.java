package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Order;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

// Monta o rascunho do pedido: preços, código, status e datas são preenchidos pelo service.
// addressId não faz parte do modelo (o pedido guarda a cópia do endereço) e vai à parte.
@ApplicationScoped
public class OrderRequestMapper {

    @Inject OrderItemRequestMapper itemMapper;

    public Order toModel(OrderRequest request) {
        OrderAddressRequest address = request.address();
        return new Order(
                null,
                null,
                request.clientId(),
                null,
                null,
                request.guestName(),
                request.guestPhone(),
                request.fulfillmentType(),
                null,
                request.paymentMethod(),
                false,
                null,
                address == null ? null : address.streetAddress(),
                address == null ? null : address.number(),
                address == null ? null : address.cep(),
                address == null ? null : address.complement(),
                request.notes(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                itemMapper.toModels(request.items()));
    }
}

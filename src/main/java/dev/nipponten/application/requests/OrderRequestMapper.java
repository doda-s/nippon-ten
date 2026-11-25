package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Address;
import dev.nipponten.domain.models.Order;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

// Monta o rascunho do pedido: preços, código, status e datas são preenchidos pelo service.
// O pedido de pontos vai em pointsRedeemed. addressId não faz parte do modelo (o pedido guarda a
// cópia do endereço) e vai à parte.
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
                request.guestEmail(),
                request.fulfillmentType(),
                null,
                request.paymentMethod(),
                request.paymentChannel() == null
                        ? Order.PaymentChannel.IN_PERSON
                        : request.paymentChannel(),
                false,
                null,
                address == null ? null : address.streetAddress(),
                address == null ? null : address.number(),
                address == null ? null : address.cep(),
                address == null ? null : address.complement(),
                null,
                null,
                null,
                request.notes(),
                null,
                null,
                null,
                null,
                null,
                null,
                request.pointsToRedeem() == null ? 0 : request.pointsToRedeem(),
                null,
                null,
                0,
                null,
                null,
                itemMapper.toModels(request.items()));
    }

    /** Endereço digitado; bairro, cidade e UF são preenchidos pelo service a partir do CEP. */
    public static Address toAddress(OrderAddressRequest request) {
        return request == null
                ? null
                : new Address(
                        request.streetAddress(),
                        request.number(),
                        request.complement(),
                        request.cep(),
                        null,
                        null,
                        null);
    }
}

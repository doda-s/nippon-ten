package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Address;

// Endereço da loja com bairro, cidade e UF do CEP, para o admin conferir o que foi achado.
public record StoreAddressResponse(
        String streetAddress,
        String number,
        String complement,
        String cep,
        String neighborhood,
        String city,
        String state) {

    public static StoreAddressResponse of(Address address) {
        return address == null
                ? null
                : new StoreAddressResponse(
                        address.streetAddress(),
                        address.number(),
                        address.complement(),
                        address.cep(),
                        address.neighborhood(),
                        address.city(),
                        address.state());
    }
}

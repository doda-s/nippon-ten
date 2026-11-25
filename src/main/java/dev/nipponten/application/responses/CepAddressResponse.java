package dev.nipponten.application.responses;

import dev.nipponten.domain.models.CepAddress;

// street e neighborhood são nulos no CEP único de cidade pequena: o front pede a rua ao cliente.
public record CepAddressResponse(
        String cep, String street, String neighborhood, String city, String state) {

    public static CepAddressResponse of(CepAddress address) {
        String digits = address.cep();
        return new CepAddressResponse(
                digits.substring(0, 5) + "-" + digits.substring(5),
                address.street(),
                address.neighborhood(),
                address.city(),
                address.state());
    }
}

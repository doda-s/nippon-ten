package dev.nipponten.application.responses;

import dev.nipponten.domain.models.LocatedAddress;

/** location nulo = o endereço não foi achado com precisão de rua: o admin põe o pino à mão. */
public record StoreGeocodeResponse(StoreAddressResponse address, StoreLocationResponse location) {

    public static StoreGeocodeResponse of(LocatedAddress located) {
        return new StoreGeocodeResponse(
                StoreAddressResponse.of(located.address()),
                StoreLocationResponse.of(located.coordinates()));
    }
}

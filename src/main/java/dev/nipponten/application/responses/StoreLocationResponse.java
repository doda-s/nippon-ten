package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Coordinates;

public record StoreLocationResponse(double latitude, double longitude) {

    public static StoreLocationResponse of(Coordinates coordinates) {
        return coordinates == null
                ? null
                : new StoreLocationResponse(coordinates.latitude(), coordinates.longitude());
    }
}

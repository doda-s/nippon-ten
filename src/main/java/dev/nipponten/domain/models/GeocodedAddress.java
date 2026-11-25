package dev.nipponten.domain.models;

import java.time.Instant;

/** Resultado em cache de uma geocodificação. coordinates nulo = o provedor não achou o endereço. */
public record GeocodedAddress(String addressHash, Coordinates coordinates, Instant geocodedAt) {}

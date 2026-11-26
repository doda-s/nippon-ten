package dev.nipponten.domain.models;

/** Endereço completado pelo CEP e a coordenada achada para ele. coordinates nulo = não achou. */
public record LocatedAddress(Address address, Coordinates coordinates) {}

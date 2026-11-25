package dev.nipponten.domain.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Configuração única da entrega. storeLocation nulo = cálculo desligado (taxa 0). Cada faixa cobra
 * fee até maxDistanceKm; a maior faixa é o raio de entrega.
 */
public record DeliverySettings(
        Coordinates storeLocation, List<Tier> tiers, Instant updatedAt, Long updatedByUserId) {

    public record Tier(BigDecimal maxDistanceKm, BigDecimal fee) {}
}

package dev.nipponten.domain.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Configuração única da entrega. storeAddress é o endereço da loja, já com bairro, cidade e UF do
 * CEP. pinnedLocation é o pino posto à mão no mapa e, se existir, vale no lugar da coordenada do
 * endereço. Sem nenhum dos dois o cálculo fica desligado (taxa 0). Cada faixa cobra fee até
 * maxDistanceKm; a maior faixa é o raio de entrega.
 */
public record DeliverySettings(
        Address storeAddress,
        Coordinates pinnedLocation,
        List<Tier> tiers,
        Instant updatedAt,
        Long updatedByUserId) {

    public static final DeliverySettings OFF =
            new DeliverySettings(null, null, List.of(), null, null);

    public boolean enabled() {
        return storeAddress != null || pinnedLocation != null;
    }

    public record Tier(BigDecimal maxDistanceKm, BigDecimal fee) {}
}

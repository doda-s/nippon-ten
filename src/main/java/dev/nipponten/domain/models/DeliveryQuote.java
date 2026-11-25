package dev.nipponten.domain.models;

import java.math.BigDecimal;

/** Taxa de entrega de um endereço. distanceKm é nulo quando não houve cálculo. */
public record DeliveryQuote(BigDecimal distanceKm, BigDecimal fee) {}

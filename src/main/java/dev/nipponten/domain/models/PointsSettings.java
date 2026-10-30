package dev.nipponten.domain.models;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Configuração única do programa de pontos. earnRate = pontos por R$1 pago; pointValue = R$ que um
 * ponto vale no resgate; expirationDays nulo = pontos não expiram.
 */
public record PointsSettings(
        BigDecimal earnRate,
        BigDecimal pointValue,
        Integer expirationDays,
        Instant updatedAt,
        Long updatedByUserId) {}

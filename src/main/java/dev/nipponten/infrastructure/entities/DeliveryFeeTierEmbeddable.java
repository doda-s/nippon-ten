package dev.nipponten.infrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public class DeliveryFeeTierEmbeddable {

    @Column(nullable = false)
    private BigDecimal maxDistanceKm;

    @Column(nullable = false)
    private BigDecimal fee;

    public DeliveryFeeTierEmbeddable() {}

    public DeliveryFeeTierEmbeddable(BigDecimal maxDistanceKm, BigDecimal fee) {
        this.maxDistanceKm = maxDistanceKm;
        this.fee = fee;
    }

    public BigDecimal getMaxDistanceKm() {
        return maxDistanceKm;
    }

    public BigDecimal getFee() {
        return fee;
    }
}

package dev.nipponten.infrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

// Linha única (id = 1), criada pela migration.
@Entity
@Table(name = "points_settings")
public class PointsSettingsEntity {

    public static final long SINGLETON_ID = 1L;

    @Id private Long id;

    @Column(nullable = false)
    private BigDecimal earnRate;

    @Column(nullable = false)
    private BigDecimal pointValue;

    private Integer expirationDays;

    @Column(nullable = false)
    private Instant updatedAt;

    private Long updatedByUserId;

    public PointsSettingsEntity() {}

    public Long getId() {
        return id;
    }

    public BigDecimal getEarnRate() {
        return earnRate;
    }

    public void setEarnRate(BigDecimal earnRate) {
        this.earnRate = earnRate;
    }

    public BigDecimal getPointValue() {
        return pointValue;
    }

    public void setPointValue(BigDecimal pointValue) {
        this.pointValue = pointValue;
    }

    public Integer getExpirationDays() {
        return expirationDays;
    }

    public void setExpirationDays(Integer expirationDays) {
        this.expirationDays = expirationDays;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getUpdatedByUserId() {
        return updatedByUserId;
    }

    public void setUpdatedByUserId(Long updatedByUserId) {
        this.updatedByUserId = updatedByUserId;
    }
}

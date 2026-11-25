package dev.nipponten.infrastructure.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// Linha única (id = 1), criada pela migration. As faixas não têm identidade própria: são valores
// da configuração, substituídos inteiros a cada alteração.
@Entity
@Table(name = "delivery_settings")
public class DeliverySettingsEntity {

    public static final long SINGLETON_ID = 1L;

    @Id private Long id;

    private Double storeLatitude;

    private Double storeLongitude;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "delivery_fee_tier",
            joinColumns = @JoinColumn(name = "delivery_settings_id"))
    @OrderBy("maxDistanceKm")
    private List<DeliveryFeeTierEmbeddable> tiers = new ArrayList<>();

    @Column(nullable = false)
    private Instant updatedAt;

    private Long updatedByUserId;

    public DeliverySettingsEntity() {}

    public Long getId() {
        return id;
    }

    public Double getStoreLatitude() {
        return storeLatitude;
    }

    public void setStoreLatitude(Double storeLatitude) {
        this.storeLatitude = storeLatitude;
    }

    public Double getStoreLongitude() {
        return storeLongitude;
    }

    public void setStoreLongitude(Double storeLongitude) {
        this.storeLongitude = storeLongitude;
    }

    public List<DeliveryFeeTierEmbeddable> getTiers() {
        return tiers;
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

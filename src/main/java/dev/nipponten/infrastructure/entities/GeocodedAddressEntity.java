package dev.nipponten.infrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "geocoded_address")
public class GeocodedAddressEntity {

    @Id private String addressHash;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false)
    private Instant geocodedAt;

    public GeocodedAddressEntity() {}

    public String getAddressHash() {
        return addressHash;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Instant getGeocodedAt() {
        return geocodedAt;
    }
}

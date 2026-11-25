package dev.nipponten.infrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "cep_address")
public class CepAddressEntity {

    @Id private String cep;

    private String street;

    private String neighborhood;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private Instant fetchedAt;

    public CepAddressEntity() {}

    public String getCep() {
        return cep;
    }

    public String getStreet() {
        return street;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }
}

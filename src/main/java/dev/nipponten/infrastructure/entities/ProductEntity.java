package dev.nipponten.infrastructure.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product")
public class ProductEntity {

    public enum Status { ACTIVE, INACTIVE }

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private String imageUrl;

    private String description;

    @Enumerated(EnumType.STRING)
    private Status status;

    protected ProductEntity() {}

    protected ProductEntity(String name, String imageUrl, String description, Status status) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.description = description;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}

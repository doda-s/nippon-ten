package dev.nipponten.infrastructure.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "promotion_type")
public class PromotionTypeEntity {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private String description;

    private String type;

    private BigDecimal value;

    protected PromotionTypeEntity() {}

    protected PromotionTypeEntity(String name, String description, String type, BigDecimal value) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.value = value;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }
}

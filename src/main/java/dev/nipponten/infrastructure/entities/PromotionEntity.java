package dev.nipponten.infrastructure.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotion")
public class PromotionEntity {

    public enum Status { ACTIVE, INACTIVE }

    @Id
    @GeneratedValue
    private Long id;

    private String title;

    private BigDecimal price;

    private String imageUrl;

    private String description;

    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne
    @JoinColumn(name = "promotion_type_id")
    private PromotionTypeEntity promotionType;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private boolean enablePromotionPoints;

    public PromotionEntity() {}

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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

    public PromotionTypeEntity getPromotionType() {
        return promotionType;
    }

    public void setPromotionType(PromotionTypeEntity promotionType) {
        this.promotionType = promotionType;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public void setProduct(ProductEntity product) {
        this.product = product;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public boolean isEnablePromotionPoints() {
        return enablePromotionPoints;
    }

    public void setEnablePromotionPoints(boolean enablePromotionPoints) {
        this.enablePromotionPoints = enablePromotionPoints;
    }
}

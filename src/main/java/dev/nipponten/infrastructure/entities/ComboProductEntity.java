package dev.nipponten.infrastructure.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "combo_product")
public class ComboProductEntity {

    @Id @GeneratedValue private Long id;

    @ManyToOne
    @JoinColumn(name = "combo_id")
    private ComboEntity combo;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    public ComboProductEntity() {}

    public Long getId() {
        return id;
    }

    public ComboEntity getCombo() {
        return combo;
    }

    public void setCombo(ComboEntity combo) {
        this.combo = combo;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public void setProduct(ProductEntity product) {
        this.product = product;
    }
}

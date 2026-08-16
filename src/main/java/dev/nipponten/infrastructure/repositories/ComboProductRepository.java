package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.ComboProduct;
import java.util.List;

public interface ComboProductRepository {
    ComboProduct save(ComboProduct model);
    void remove(ComboProduct model);
    ComboProduct getById(Long id);
    List<ComboProduct> getAll();
    java.util.List<ComboProduct> getByCombo(Long comboId);
    java.util.List<ComboProduct> getByProduct(Long productId);
}

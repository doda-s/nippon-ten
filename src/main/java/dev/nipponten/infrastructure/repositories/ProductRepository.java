package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.Product;
import java.util.List;

public interface ProductRepository {
    Product save(Product model);
    void remove(Product model);
    Product getById(Long id);
    List<Product> getAll();
}

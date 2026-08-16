package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.ProductSize;
import java.util.List;

public interface ProductSizeRepository {
    ProductSize save(ProductSize model);
    void remove(ProductSize model);
    ProductSize getById(Long id);
    List<ProductSize> getAll();
    java.util.List<ProductSize> getByProduct(Long productId);
}

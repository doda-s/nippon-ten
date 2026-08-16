package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.ProductIngredient;
import java.util.List;

public interface ProductIngredientRepository {
    ProductIngredient save(ProductIngredient model);

    void remove(ProductIngredient model);

    ProductIngredient getById(Long id);

    List<ProductIngredient> getAll();

    java.util.List<ProductIngredient> getByProduct(Long productId);

    java.util.List<ProductIngredient> getByIngredient(Long ingredientId);
}

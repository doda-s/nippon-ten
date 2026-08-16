package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.AdditionalIngredient;
import java.util.List;

public interface AdditionalIngredientRepository {
    AdditionalIngredient save(AdditionalIngredient model);

    void remove(AdditionalIngredient model);

    AdditionalIngredient getById(Long id);

    List<AdditionalIngredient> getAll();

    java.util.List<AdditionalIngredient> getByProduct(Long productId);

    java.util.List<AdditionalIngredient> getByIngredient(Long ingredientId);
}

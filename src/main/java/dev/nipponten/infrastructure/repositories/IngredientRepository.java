package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.Ingredient;
import java.util.List;

public interface IngredientRepository {
    Ingredient save(Ingredient model);
    void remove(Ingredient model);
    Ingredient getById(Long id);
    List<Ingredient> getAll();
}

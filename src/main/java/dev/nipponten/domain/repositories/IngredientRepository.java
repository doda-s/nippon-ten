package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.Ingredient;
import dev.nipponten.domain.models.PageResult;
import java.util.List;

public interface IngredientRepository {
    Ingredient save(Ingredient model);

    void remove(Ingredient model);

    Ingredient getById(Long id);

    List<Ingredient> getAll();

    PageResult<Ingredient> getPage(int page, int size);
}

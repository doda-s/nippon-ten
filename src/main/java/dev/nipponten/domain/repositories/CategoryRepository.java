package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.Category;
import dev.nipponten.domain.models.PageResult;

public interface CategoryRepository {
    Category save(Category model);

    void remove(Category model);

    Category getById(Long id);

    // Comparação sem diferenciar maiúsculas: "Pastéis" e "pastéis" são a mesma categoria.
    Category getByName(String name);

    // Ordenada por displayOrder e depois por id, a ordem em que o cardápio mostra as seções.
    PageResult<Category> getPage(int page, int size);
}

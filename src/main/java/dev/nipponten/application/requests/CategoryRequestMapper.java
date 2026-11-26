package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Category;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CategoryRequestMapper {

    public Category toModel(Long id, CategoryRequest request) {
        return new Category(
                id,
                request.name().trim(),
                request.displayOrder() == null ? 0 : request.displayOrder());
    }
}

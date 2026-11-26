package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Category;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CategoryResponseMapper {

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.id(), category.name(), category.displayOrder());
    }
}

package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.Category;
import dev.nipponten.infrastructure.entities.CategoryEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CategoryMapper {

    public CategoryEntity toEntity(Category model) {
        CategoryEntity entity = new CategoryEntity();
        applyToEntity(entity, model);
        return entity;
    }

    public void updateEntity(CategoryEntity entity, Category model) {
        applyToEntity(entity, model);
    }

    private void applyToEntity(CategoryEntity entity, Category model) {
        entity.setName(model.name());
        entity.setDisplayOrder(model.displayOrder());
    }

    public Category toModel(CategoryEntity entity) {
        return new Category(entity.getId(), entity.getName(), entity.getDisplayOrder());
    }
}

package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.CategoryNotFoundException;
import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.domain.models.Category;
import dev.nipponten.domain.models.PageResult;
import dev.nipponten.domain.repositories.CategoryRepository;
import dev.nipponten.domain.repositories.ProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CategoryService {

    @Inject CategoryRepository repository;

    // Repositório, e não o service, para não criar ciclo: o ProductService valida a categoria aqui.
    @Inject ProductRepository productRepository;

    public Category create(Category model) {
        requireUniqueName(model);
        return repository.save(model);
    }

    public Category getById(Long id) {
        Category model = repository.getById(id);
        if (model == null) throw new CategoryNotFoundException(id);
        return model;
    }

    public PageResult<Category> getPage(int page, int size) {
        return repository.getPage(page, size);
    }

    public Category update(Long id, Category model) {
        getById(id);
        requireUniqueName(model);
        return repository.save(model);
    }

    // Produto da categoria bloqueia a exclusão: quem gerencia o cardápio move os produtos antes,
    // em vez de vê-los sumirem da vitrine sem categoria.
    public void delete(Long id) {
        Category model = getById(id);
        long products = productRepository.countByCategory(id);
        if (products > 0) {
            throw new InvalidRequestException(
                    "Category " + id + " cannot be deleted: used by " + products + " product(s)");
        }
        repository.remove(model);
    }

    private void requireUniqueName(Category model) {
        Category existing = repository.getByName(model.name());
        if (existing != null && !existing.id().equals(model.id())) {
            throw new InvalidRequestException("Category already exists: " + model.name());
        }
    }
}

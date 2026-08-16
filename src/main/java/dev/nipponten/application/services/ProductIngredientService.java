package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.ProductIngredientNotFoundException;
import dev.nipponten.domain.models.ProductIngredient;
import dev.nipponten.infrastructure.repositories.ProductIngredientRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class ProductIngredientService {

    @Inject ProductIngredientRepository repository;

    public ProductIngredient create(ProductIngredient model) {
        return repository.save(model);
    }

    public ProductIngredient getById(Long id) {
        ProductIngredient model = repository.getById(id);
        if (model == null) throw new ProductIngredientNotFoundException(id);
        return model;
    }

    public List<ProductIngredient> getAll() {
        return repository.getAll();
    }

    public ProductIngredient update(Long id, ProductIngredient model) {
        getById(id);
        return repository.save(model);
    }

    public void delete(Long id) {
        ProductIngredient model = getById(id);
        repository.remove(model);
    }

    public List<ProductIngredient> getByProduct(Long productId) {
        return repository.getByProduct(productId);
    }

    public List<ProductIngredient> getByIngredient(Long ingredientId) {
        return repository.getByIngredient(ingredientId);
    }
}

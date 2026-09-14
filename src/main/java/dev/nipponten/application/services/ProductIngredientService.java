package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.ProductIngredientNotFoundException;
import dev.nipponten.domain.models.ProductIngredient;
import dev.nipponten.domain.repositories.ProductIngredientRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class ProductIngredientService {

    @Inject ProductIngredientRepository repository;

    @Inject ProductService productService;

    public ProductIngredient create(ProductIngredient model) {
        productService.getById(model.productId());
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

    public ProductIngredient update(Long productId, Long id, ProductIngredient model) {
        requireByProduct(productId, id);
        return repository.save(model);
    }

    public void delete(Long productId, Long id) {
        ProductIngredient model = requireByProduct(productId, id);
        repository.remove(model);
    }

    public List<ProductIngredient> getByProduct(Long productId) {
        productService.getById(productId);
        return repository.getByProduct(productId);
    }

    public List<ProductIngredient> getByIngredient(Long ingredientId) {
        return repository.getByIngredient(ingredientId);
    }

    public ProductIngredient requireByProduct(Long productId, Long id) {
        productService.getById(productId);
        ProductIngredient model = getById(id);
        if (!productId.equals(model.productId())) {
            throw new ProductIngredientNotFoundException(id);
        }
        return model;
    }
}

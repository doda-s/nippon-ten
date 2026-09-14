package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.AdditionalIngredientNotFoundException;
import dev.nipponten.domain.models.AdditionalIngredient;
import dev.nipponten.domain.repositories.AdditionalIngredientRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class AdditionalIngredientService {

    @Inject AdditionalIngredientRepository repository;

    @Inject ProductService productService;

    public AdditionalIngredient create(AdditionalIngredient model) {
        productService.getById(model.productId());
        return repository.save(model);
    }

    public AdditionalIngredient getById(Long id) {
        AdditionalIngredient model = repository.getById(id);
        if (model == null) throw new AdditionalIngredientNotFoundException(id);
        return model;
    }

    public List<AdditionalIngredient> getAll() {
        return repository.getAll();
    }

    public AdditionalIngredient update(Long productId, Long id, AdditionalIngredient model) {
        requireByProduct(productId, id);
        return repository.save(model);
    }

    public void delete(Long productId, Long id) {
        AdditionalIngredient model = requireByProduct(productId, id);
        repository.remove(model);
    }

    public List<AdditionalIngredient> getByProduct(Long productId) {
        productService.getById(productId);
        return repository.getByProduct(productId);
    }

    public List<AdditionalIngredient> getByIngredient(Long ingredientId) {
        return repository.getByIngredient(ingredientId);
    }

    public AdditionalIngredient requireByProduct(Long productId, Long id) {
        productService.getById(productId);
        AdditionalIngredient model = getById(id);
        if (!productId.equals(model.productId())) {
            throw new AdditionalIngredientNotFoundException(id);
        }
        return model;
    }
}

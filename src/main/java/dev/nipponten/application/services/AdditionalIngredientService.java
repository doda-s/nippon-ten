package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.AdditionalIngredientNotFoundException;
import dev.nipponten.domain.models.AdditionalIngredient;
import dev.nipponten.infrastructure.repositories.AdditionalIngredientRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class AdditionalIngredientService {

    @Inject
    AdditionalIngredientRepository repository;

    public AdditionalIngredient create(AdditionalIngredient model) {
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

    public AdditionalIngredient update(Long id, AdditionalIngredient model) {
        getById(id);
        return repository.save(model);
    }

    public void delete(Long id) {
        AdditionalIngredient model = getById(id);
        repository.remove(model);
    }

    public List<AdditionalIngredient> getByProduct(Long productId) {
        return repository.getByProduct(productId);
    }

    public List<AdditionalIngredient> getByIngredient(Long ingredientId) {
        return repository.getByIngredient(ingredientId);
    }
}

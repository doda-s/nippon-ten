package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.IngredientNotFoundException;
import dev.nipponten.domain.models.Ingredient;
import dev.nipponten.infrastructure.repositories.IngredientRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class IngredientService {

    @Inject
    IngredientRepository repository;

    public Ingredient create(Ingredient model) {
        return repository.save(model);
    }

    public Ingredient getById(Long id) {
        Ingredient model = repository.getById(id);
        if (model == null) throw new IngredientNotFoundException(id);
        return model;
    }

    public List<Ingredient> getAll() {
        return repository.getAll();
    }

    public Ingredient update(Long id, Ingredient model) {
        getById(id);
        return repository.save(model);
    }

    public void delete(Long id) {
        Ingredient model = getById(id);
        repository.remove(model);
    }
}

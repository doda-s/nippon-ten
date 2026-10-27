package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.AdditionalIngredientNotFoundException;
import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.domain.models.AdditionalIngredient;
import dev.nipponten.domain.repositories.AdditionalIngredientRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.List;

@ApplicationScoped
public class AdditionalIngredientService {

    @Inject AdditionalIngredientRepository repository;

    @Inject ProductService productService;

    @Inject IngredientService ingredientService;

    public AdditionalIngredient create(AdditionalIngredient model) {
        productService.getById(model.productId());
        ingredientService.getById(model.ingredientId());
        return repository.save(model);
    }

    // Mesma lógica de ProductService.register: lote atômico, nada é criado se algum ingrediente
    // não existir ou se houver repetidos no próprio request.
    @Transactional
    public List<AdditionalIngredient> createBatch(
            Long productId, List<AdditionalIngredient> models) {
        productService.getById(productId);
        requireNoDuplicates(models);
        models.forEach(model -> ingredientService.getById(model.ingredientId()));
        return models.stream().map(repository::save).toList();
    }

    private static void requireNoDuplicates(List<AdditionalIngredient> models) {
        var seen = new HashSet<Long>();
        models.stream()
                .map(AdditionalIngredient::ingredientId)
                .filter(value -> !seen.add(value))
                .findFirst()
                .ifPresent(
                        value -> {
                            throw new InvalidRequestException(
                                    "Duplicated ingredient in product: " + value);
                        });
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
        ingredientService.getById(model.ingredientId());
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

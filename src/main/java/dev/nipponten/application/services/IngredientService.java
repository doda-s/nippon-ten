package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.IngredientNotFoundException;
import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.domain.models.AdditionalIngredient;
import dev.nipponten.domain.models.Ingredient;
import dev.nipponten.domain.models.Product;
import dev.nipponten.domain.models.ProductIngredient;
import dev.nipponten.domain.repositories.AdditionalIngredientRepository;
import dev.nipponten.domain.repositories.IngredientRepository;
import dev.nipponten.domain.repositories.ProductIngredientRepository;
import dev.nipponten.domain.repositories.ProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class IngredientService {

    @Inject IngredientRepository repository;

    @Inject ProductIngredientRepository productIngredientRepository;

    @Inject AdditionalIngredientRepository additionalIngredientRepository;

    // Repositório, e não o service, para não criar ciclo IngredientService <-> ProductService.
    @Inject ProductRepository productRepository;

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
        int products = productIngredientRepository.getByIngredient(id).size();
        int additionals = additionalIngredientRepository.getByIngredient(id).size();
        if (products > 0 || additionals > 0) {
            throw new InvalidRequestException(
                    "Ingredient "
                            + id
                            + " cannot be deleted: used by "
                            + products
                            + " product ingredient(s) and "
                            + additionals
                            + " additional ingredient(s)");
        }
        repository.remove(model);
    }

    // Ao marcar um ingrediente como esgotado, o usuário interno precisa da lista de produtos que
    // dependem dele para decidir, manualmente, quais também ficam indisponíveis. A propagação de
    // status nunca é automática.
    public List<Product> getProductsUsingAsIngredient(Long id) {
        getById(id);
        return resolveProducts(
                productIngredientRepository.getByIngredient(id).stream()
                        .map(ProductIngredient::productId)
                        .toList());
    }

    public List<Product> getProductsOfferingAsAdditional(Long id) {
        getById(id);
        return resolveProducts(
                additionalIngredientRepository.getByIngredient(id).stream()
                        .map(AdditionalIngredient::productId)
                        .toList());
    }

    private List<Product> resolveProducts(List<Long> productIds) {
        return productIds.stream()
                .distinct()
                .map(productRepository::getById)
                .filter(Objects::nonNull)
                .toList();
    }
}

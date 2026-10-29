package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.application.exceptions.ProductIngredientNotFoundException;
import dev.nipponten.domain.models.ProductIngredient;
import dev.nipponten.domain.repositories.ProductIngredientRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.List;

@ApplicationScoped
public class ProductIngredientService {

    @Inject ProductIngredientRepository repository;

    @Inject ProductService productService;

    @Inject IngredientService ingredientService;

    public ProductIngredient create(ProductIngredient model) {
        productService.getById(model.productId());
        ingredientService.getById(model.ingredientId());
        return repository.save(model);
    }

    // Mesma lógica de ProductService.register: lote atômico, nada é criado se algum ingrediente
    // não existir ou se houver repetidos no próprio request.
    @Transactional
    public List<ProductIngredient> createBatch(Long productId, List<ProductIngredient> models) {
        productService.getById(productId);
        requireNoDuplicates(models);
        models.forEach(model -> ingredientService.getById(model.ingredientId()));
        return models.stream().map(repository::save).toList();
    }

    private static void requireNoDuplicates(List<ProductIngredient> models) {
        var seen = new HashSet<Long>();
        models.stream()
                .map(ProductIngredient::ingredientId)
                .filter(value -> !seen.add(value))
                .findFirst()
                .ifPresent(
                        value -> {
                            throw new InvalidRequestException(
                                    "Duplicated ingredient in product: " + value);
                        });
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
        ingredientService.getById(model.ingredientId());
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

package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.ProductSizeNotFoundException;
import dev.nipponten.domain.models.ProductSize;
import dev.nipponten.infrastructure.repositories.ProductSizeRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class ProductSizeService {

    @Inject ProductSizeRepository repository;

    public ProductSize create(ProductSize model) {
        return repository.save(model);
    }

    public ProductSize getById(Long id) {
        ProductSize model = repository.getById(id);
        if (model == null) throw new ProductSizeNotFoundException(id);
        return model;
    }

    public List<ProductSize> getAll() {
        return repository.getAll();
    }

    public ProductSize update(Long id, ProductSize model) {
        getById(id);
        return repository.save(model);
    }

    public void delete(Long id) {
        ProductSize model = getById(id);
        repository.remove(model);
    }

    public List<ProductSize> getByProduct(Long productId) {
        return repository.getByProduct(productId);
    }
}

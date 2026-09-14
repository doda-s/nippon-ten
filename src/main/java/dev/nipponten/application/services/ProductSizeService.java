package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.ProductSizeNotFoundException;
import dev.nipponten.domain.models.ProductSize;
import dev.nipponten.domain.repositories.ProductSizeRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class ProductSizeService {

    @Inject ProductSizeRepository repository;

    @Inject ProductService productService;

    public ProductSize create(ProductSize model) {
        productService.getById(model.productId());
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

    public ProductSize update(Long productId, Long id, ProductSize model) {
        requireByProduct(productId, id);
        return repository.save(model);
    }

    public void delete(Long productId, Long id) {
        ProductSize model = requireByProduct(productId, id);
        repository.remove(model);
    }

    public List<ProductSize> getByProduct(Long productId) {
        productService.getById(productId);
        return repository.getByProduct(productId);
    }

    public ProductSize requireByProduct(Long productId, Long id) {
        productService.getById(productId);
        ProductSize model = getById(id);
        if (!productId.equals(model.productId())) {
            throw new ProductSizeNotFoundException(id);
        }
        return model;
    }
}

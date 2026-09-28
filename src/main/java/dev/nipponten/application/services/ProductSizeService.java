package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.application.exceptions.ProductSizeNotFoundException;
import dev.nipponten.domain.models.ProductSize;
import dev.nipponten.domain.repositories.ProductSizeRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class ProductSizeService {

    @Inject ProductSizeRepository repository;

    @Inject ProductService productService;

    @Inject SizeService sizeService;

    public ProductSize create(ProductSize model) {
        productService.getById(model.productId());
        sizeService.getById(model.sizeId());
        requireSizeNotTaken(model, null);
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
        sizeService.getById(model.sizeId());
        requireSizeNotTaken(model, id);
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

    // O mesmo tamanho só pode ser associado uma vez a cada produto: é essa combinação que
    // carrega o preço.
    private void requireSizeNotTaken(ProductSize model, Long productSizeId) {
        repository.getByProduct(model.productId()).stream()
                .filter(existing -> existing.sizeId().equals(model.sizeId()))
                .filter(existing -> !Objects.equals(existing.id(), productSizeId))
                .findFirst()
                .ifPresent(
                        existing -> {
                            throw new InvalidRequestException(
                                    "Product "
                                            + model.productId()
                                            + " already has size "
                                            + model.sizeId());
                        });
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

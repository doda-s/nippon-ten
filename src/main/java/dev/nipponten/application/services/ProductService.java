package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.ProductNotFoundException;
import dev.nipponten.domain.models.Product;
import dev.nipponten.infrastructure.repositories.ProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class ProductService {

    @Inject
    ProductRepository repository;

    public Product create(Product model) {
        return repository.save(model);
    }

    public Product getById(Long id) {
        Product model = repository.getById(id);
        if (model == null) throw new ProductNotFoundException(id);
        return model;
    }

    public List<Product> getAll() {
        return repository.getAll();
    }

    public Product update(Long id, Product model) {
        getById(id);
        return repository.save(model);
    }

    public void delete(Long id) {
        Product model = getById(id);
        repository.remove(model);
    }
}

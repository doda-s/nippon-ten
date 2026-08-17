package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.ComboProductNotFoundException;
import dev.nipponten.domain.models.ComboProduct;
import dev.nipponten.infrastructure.repositories.ComboProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class ComboProductService {

    @Inject ComboProductRepository repository;

    public ComboProduct create(ComboProduct model) {
        return repository.save(model);
    }

    public ComboProduct getById(Long id) {
        ComboProduct model = repository.getById(id);
        if (model == null) throw new ComboProductNotFoundException(id);
        return model;
    }

    public List<ComboProduct> getAll() {
        return repository.getAll();
    }

    public ComboProduct update(Long id, ComboProduct model) {
        getById(id);
        return repository.save(model);
    }

    public void delete(Long id) {
        ComboProduct model = getById(id);
        repository.remove(model);
    }

    public List<ComboProduct> getByCombo(Long comboId) {
        return repository.getByCombo(comboId);
    }

    public List<ComboProduct> getByProduct(Long productId) {
        return repository.getByProduct(productId);
    }
}

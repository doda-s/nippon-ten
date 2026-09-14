package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.ComboProductNotFoundException;
import dev.nipponten.domain.models.ComboProduct;
import dev.nipponten.domain.repositories.ComboProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class ComboProductService {

    @Inject ComboProductRepository repository;

    @Inject ComboService comboService;

    public ComboProduct create(ComboProduct model) {
        comboService.getById(model.comboId());
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

    public ComboProduct update(Long comboId, Long id, ComboProduct model) {
        requireByCombo(comboId, id);
        return repository.save(model);
    }

    public void delete(Long comboId, Long id) {
        ComboProduct model = requireByCombo(comboId, id);
        repository.remove(model);
    }

    public List<ComboProduct> getByCombo(Long comboId) {
        comboService.getById(comboId);
        return repository.getByCombo(comboId);
    }

    public List<ComboProduct> getByProduct(Long productId) {
        return repository.getByProduct(productId);
    }

    public ComboProduct requireByCombo(Long comboId, Long id) {
        comboService.getById(comboId);
        ComboProduct model = getById(id);
        if (!comboId.equals(model.comboId())) {
            throw new ComboProductNotFoundException(id);
        }
        return model;
    }
}

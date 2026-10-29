package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.ComboProductNotFoundException;
import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.domain.models.ComboProduct;
import dev.nipponten.domain.repositories.ComboProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class ComboProductService {

    @Inject ComboProductRepository repository;

    @Inject ComboService comboService;

    public ComboProduct create(ComboProduct model) {
        comboService.getById(model.comboId());
        comboService.requireValidProduct(model);
        requireNotInCombo(model, null);
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
        ComboProduct current = requireByCombo(comboId, id);
        comboService.requireValidProduct(model);
        requireNotInCombo(model, id);
        comboService.requireMinimumProducts(
                totalQuantity(comboId) - current.quantity() + model.quantity());
        return repository.save(model);
    }

    public void delete(Long comboId, Long id) {
        ComboProduct model = requireByCombo(comboId, id);
        comboService.requireMinimumProducts(totalQuantity(comboId) - model.quantity());
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

    private int totalQuantity(Long comboId) {
        return repository.getByCombo(comboId).stream().mapToInt(ComboProduct::quantity).sum();
    }

    // A mesma combinação produto+tamanho aparece uma vez por combo; a repetição é a quantidade.
    private void requireNotInCombo(ComboProduct model, Long comboProductId) {
        repository.getByCombo(model.comboId()).stream()
                .filter(existing -> existing.productId().equals(model.productId()))
                .filter(existing -> Objects.equals(existing.productSizeId(), model.productSizeId()))
                .filter(existing -> !Objects.equals(existing.id(), comboProductId))
                .findFirst()
                .ifPresent(
                        existing -> {
                            throw new InvalidRequestException(
                                    "Combo "
                                            + model.comboId()
                                            + " already has product "
                                            + model.productId()
                                            + " with size "
                                            + model.productSizeId()
                                            + ": change its quantity instead");
                        });
    }
}

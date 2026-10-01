package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.ComboNotFoundException;
import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.domain.models.Combo;
import dev.nipponten.domain.models.ComboProduct;
import dev.nipponten.domain.repositories.ComboProductRepository;
import dev.nipponten.domain.repositories.ComboRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ComboService {

    @Inject ComboRepository repository;

    @Inject ComboProductRepository comboProductRepository;

    @Inject ProductService productService;

    // Combo + produtos são criados juntos: a relação é feita durante o fluxo de criação e o
    // combo precisa de no mínimo dois produtos, então a operação inteira é atômica.
    @Transactional
    public Combo register(Combo model, List<Long> productIds) {
        requireValidWindow(model);
        productIds.forEach(productService::getById);
        Combo saved = repository.save(model);
        productIds.forEach(
                productId ->
                        comboProductRepository.save(new ComboProduct(null, saved.id(), productId)));
        return saved;
    }

    public Combo getById(Long id) {
        Combo model = repository.getById(id);
        if (model == null) throw new ComboNotFoundException(id);
        return model;
    }

    public List<Combo> getAll() {
        return repository.getAll();
    }

    public Combo update(Long id, Combo model) {
        getById(id);
        requireValidWindow(model);
        return repository.save(model);
    }

    // Disponível no catálogo: ativo e dentro da janela. Sem datas, vale sempre.
    public List<Combo> getAvailable(LocalDateTime now) {
        return repository.getAvailable(now);
    }

    @Transactional
    public void delete(Long id) {
        Combo model = getById(id);
        comboProductRepository.getByCombo(id).forEach(comboProductRepository::remove);
        repository.remove(model);
    }

    // O período de duração é opcional; quando informado, precisa fazer sentido.
    private void requireValidWindow(Combo model) {
        if (model.startDate() != null
                && model.endDate() != null
                && !model.endDate().isAfter(model.startDate())) {
            throw new InvalidRequestException(
                    "Combo endDate must be after startDate: "
                            + model.startDate()
                            + " -> "
                            + model.endDate());
        }
    }
}

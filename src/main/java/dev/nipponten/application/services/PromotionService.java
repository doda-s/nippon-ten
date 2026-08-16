package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.PromotionNotFoundException;
import dev.nipponten.domain.models.Promotion;
import dev.nipponten.infrastructure.repositories.PromotionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class PromotionService {

    @Inject PromotionRepository repository;

    public Promotion create(Promotion model) {
        return repository.save(model);
    }

    public Promotion getById(Long id) {
        Promotion model = repository.getById(id);
        if (model == null) throw new PromotionNotFoundException(id);
        return model;
    }

    public List<Promotion> getAll() {
        return repository.getAll();
    }

    public Promotion update(Long id, Promotion model) {
        getById(id);
        return repository.save(model);
    }

    public void delete(Long id) {
        Promotion model = getById(id);
        repository.remove(model);
    }

    public List<Promotion> getByProduct(Long productId) {
        return repository.getByProduct(productId);
    }

    public List<Promotion> getByPromotionType(Long promotionTypeId) {
        return repository.getByPromotionType(promotionTypeId);
    }
}

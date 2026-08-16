package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.PromotionTypeNotFoundException;
import dev.nipponten.domain.models.PromotionType;
import dev.nipponten.infrastructure.repositories.PromotionTypeRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class PromotionTypeService {

    @Inject PromotionTypeRepository repository;

    public PromotionType create(PromotionType model) {
        return repository.save(model);
    }

    public PromotionType getById(Long id) {
        PromotionType model = repository.getById(id);
        if (model == null) throw new PromotionTypeNotFoundException(id);
        return model;
    }

    public List<PromotionType> getAll() {
        return repository.getAll();
    }

    public PromotionType update(Long id, PromotionType model) {
        getById(id);
        return repository.save(model);
    }

    public void delete(Long id) {
        PromotionType model = getById(id);
        repository.remove(model);
    }
}

package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.application.exceptions.PromotionTypeNotFoundException;
import dev.nipponten.domain.models.PromotionType;
import dev.nipponten.domain.repositories.PromotionRepository;
import dev.nipponten.domain.repositories.PromotionTypeRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class PromotionTypeService {

    @Inject PromotionTypeRepository repository;

    @Inject PromotionRepository promotionRepository;

    public PromotionType create(PromotionType model) {
        requireValidValue(model);
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
        requireValidValue(model);
        return repository.save(model);
    }

    public void delete(Long id) {
        PromotionType model = getById(id);
        int promotions = promotionRepository.getByPromotionType(id).size();
        if (promotions > 0) {
            throw new InvalidRequestException(
                    "Promotion type "
                            + id
                            + " cannot be deleted: used by "
                            + promotions
                            + " promotion(s)");
        }
        repository.remove(model);
    }

    private void requireValidValue(PromotionType model) {
        if (model.type() == PromotionType.Type.PERCENTAGE_DISCOUNT
                && model.value().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InvalidRequestException(
                    "Percentage discount cannot exceed 100: " + model.value());
        }
    }
}

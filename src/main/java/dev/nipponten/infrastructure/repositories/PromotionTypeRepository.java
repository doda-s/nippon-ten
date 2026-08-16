package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.PromotionType;
import java.util.List;

public interface PromotionTypeRepository {
    PromotionType save(PromotionType model);
    void remove(PromotionType model);
    PromotionType getById(Long id);
    List<PromotionType> getAll();
}

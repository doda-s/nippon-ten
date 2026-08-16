package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.Promotion;
import java.util.List;

public interface PromotionRepository {
    Promotion save(Promotion model);
    void remove(Promotion model);
    Promotion getById(Long id);
    List<Promotion> getAll();
    java.util.List<Promotion> getByProduct(Long productId);
    java.util.List<Promotion> getByPromotionType(Long promotionTypeId);
}

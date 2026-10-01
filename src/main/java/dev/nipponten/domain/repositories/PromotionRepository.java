package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.Promotion;
import java.time.LocalDateTime;
import java.util.List;

public interface PromotionRepository {
    Promotion save(Promotion model);

    void remove(Promotion model);

    Promotion getById(Long id);

    List<Promotion> getAll();

    List<Promotion> getAvailable(LocalDateTime now);

    java.util.List<Promotion> getByProduct(Long productId);

    java.util.List<Promotion> getByPromotionType(Long promotionTypeId);
}

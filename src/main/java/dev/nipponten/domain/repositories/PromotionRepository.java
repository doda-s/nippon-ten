package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.PageResult;
import dev.nipponten.domain.models.Promotion;
import java.time.LocalDateTime;
import java.util.List;

public interface PromotionRepository {
    Promotion save(Promotion model);

    void remove(Promotion model);

    Promotion getById(Long id);

    List<Promotion> getAll();

    List<Promotion> getAvailable(LocalDateTime now);

    List<Promotion> getByProduct(Long productId);

    List<Promotion> getByCombo(Long comboId);

    // Promoções do item cuja janela cruza [start, end), em qualquer status.
    List<Promotion> getOverlappingByProduct(Long productId, LocalDateTime start, LocalDateTime end);

    List<Promotion> getOverlappingByCombo(Long comboId, LocalDateTime start, LocalDateTime end);

    List<Promotion> getByPromotionType(Long promotionTypeId);

    PageResult<Promotion> getPage(int page, int size);

    PageResult<Promotion> getAvailablePage(LocalDateTime now, int page, int size);
}

package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.Combo;
import dev.nipponten.domain.models.PageResult;
import java.time.LocalDateTime;
import java.util.List;

public interface ComboRepository {
    Combo save(Combo model);

    void remove(Combo model);

    Combo getById(Long id);

    List<Combo> getAll();

    List<Combo> getAvailable(LocalDateTime now);

    PageResult<Combo> getPage(int page, int size);

    PageResult<Combo> getAvailablePage(LocalDateTime now, int page, int size);
}

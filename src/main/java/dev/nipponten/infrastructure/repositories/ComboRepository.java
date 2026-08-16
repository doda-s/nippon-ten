package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.Combo;
import java.util.List;

public interface ComboRepository {
    Combo save(Combo model);
    void remove(Combo model);
    Combo getById(Long id);
    List<Combo> getAll();
}

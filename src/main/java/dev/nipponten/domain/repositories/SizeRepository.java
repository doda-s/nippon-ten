package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.Size;
import java.util.List;

public interface SizeRepository {
    Size save(Size model);

    void remove(Size model);

    Size getById(Long id);

    List<Size> getAll();
}

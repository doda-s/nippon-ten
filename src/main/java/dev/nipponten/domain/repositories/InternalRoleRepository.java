package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.InternalRole;
import java.util.List;

public interface InternalRoleRepository {
    InternalRole save(InternalRole model);

    void remove(InternalRole model);

    InternalRole getById(Long id);

    List<InternalRole> getAll();
}

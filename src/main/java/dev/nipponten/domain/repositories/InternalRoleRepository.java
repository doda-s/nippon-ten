package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.InternalRole;
import java.util.List;

public interface InternalRoleRepository {
    InternalRole save(InternalRole model);

    void remove(InternalRole model);

    InternalRole getById(Long id);

    List<InternalRole> getAll();

    InternalRole getByName(String name);

    InternalRole getDefault();

    /** A role do super_admin, ou nulo se o bootstrap ainda não rodou. */
    InternalRole getReserved();
}

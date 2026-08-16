package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.Internal;
import java.util.List;

public interface InternalRepository {
    Internal save(Internal model);
    void remove(Internal model);
    Internal getById(Long id);
    List<Internal> getAll();
    java.util.List<Internal> getByUser(Long userId);
    java.util.List<Internal> getByInternalRole(Long internalRoleId);
}

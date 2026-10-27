package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.Internal;
import dev.nipponten.domain.models.PageResult;
import java.util.List;

public interface InternalRepository {
    Internal save(Internal model);

    void remove(Internal model);

    Internal getById(Long id);

    List<Internal> getAll();

    PageResult<Internal> getPage(int page, int size);

    java.util.List<Internal> getByUser(Long userId);

    java.util.List<Internal> getByInternalRole(Long internalRoleId);

    /** Internos com a role cujo usuário está ativo. */
    long countActiveByInternalRole(Long internalRoleId);
}

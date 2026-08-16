package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.User;
import java.util.List;

public interface UserRepository {
    User save(User model);
    void remove(User model);
    User getById(Long id);
    List<User> getAll();
}

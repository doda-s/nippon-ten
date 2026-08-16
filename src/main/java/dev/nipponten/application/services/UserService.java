package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.UserNotFoundException;
import dev.nipponten.domain.models.User;
import dev.nipponten.infrastructure.repositories.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class UserService {

    @Inject
    UserRepository repository;

    public User create(User model) {
        return repository.save(model);
    }

    public User getById(Long id) {
        User model = repository.getById(id);
        if (model == null) throw new UserNotFoundException(id);
        return model;
    }

    public List<User> getAll() {
        return repository.getAll();
    }

    public User update(Long id, User model) {
        getById(id);
        return repository.save(model);
    }

    public void delete(Long id) {
        User model = getById(id);
        repository.remove(model);
    }
}

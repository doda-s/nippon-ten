package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.ComboNotFoundException;
import dev.nipponten.domain.models.Combo;
import dev.nipponten.infrastructure.repositories.ComboRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class ComboService {

    @Inject ComboRepository repository;

    public Combo create(Combo model) {
        return repository.save(model);
    }

    public Combo getById(Long id) {
        Combo model = repository.getById(id);
        if (model == null) throw new ComboNotFoundException(id);
        return model;
    }

    public List<Combo> getAll() {
        return repository.getAll();
    }

    public Combo update(Long id, Combo model) {
        getById(id);
        return repository.save(model);
    }

    public void delete(Long id) {
        Combo model = getById(id);
        repository.remove(model);
    }
}

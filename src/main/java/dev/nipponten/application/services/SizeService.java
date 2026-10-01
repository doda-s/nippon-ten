package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.application.exceptions.SizeNotFoundException;
import dev.nipponten.domain.models.Size;
import dev.nipponten.domain.repositories.ProductSizeRepository;
import dev.nipponten.domain.repositories.SizeRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class SizeService {

    @Inject SizeRepository repository;

    @Inject ProductSizeRepository productSizeRepository;

    public Size create(Size model) {
        return repository.save(model);
    }

    public Size getById(Long id) {
        Size model = repository.getById(id);
        if (model == null) throw new SizeNotFoundException(id);
        return model;
    }

    public List<Size> getAll() {
        return repository.getAll();
    }

    public Size update(Long id, Size model) {
        getById(id);
        return repository.save(model);
    }

    public void delete(Long id) {
        Size model = getById(id);
        int productSizes = productSizeRepository.getBySize(id).size();
        if (productSizes > 0) {
            throw new InvalidRequestException(
                    "Size " + id + " cannot be deleted: used by " + productSizes + " product(s)");
        }
        repository.remove(model);
    }
}

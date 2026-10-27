package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.PageResult;
import dev.nipponten.domain.models.User;
import dev.nipponten.domain.repositories.UserRepository;
import dev.nipponten.infrastructure.entities.UserEntity;
import dev.nipponten.infrastructure.mappers.UserMapper;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class PanacheUserRepository implements UserRepository, PanacheRepository<UserEntity> {

    @Inject UserMapper mapper;

    @Override
    @Transactional
    public User save(User model) {
        UserEntity entity;
        if (model.id() == null) {
            entity = mapper.toEntity(model);
            persist(entity);
        } else {
            entity = findById(model.id());
            mapper.updateEntity(entity, model);
        }
        return mapper.toModel(entity);
    }

    @Override
    @Transactional
    public void remove(User model) {
        delete(findById(model.id()));
    }

    @Override
    public User getById(Long id) {
        UserEntity entity = findById(id);
        return entity == null ? null : mapper.toModel(entity);
    }

    @Override
    public User getByEmail(String email) {
        return find("lower(email) = ?1", email.toLowerCase())
                .firstResultOptional()
                .map(mapper::toModel)
                .orElse(null);
    }

    @Override
    public List<User> getAll() {
        return listAll().stream().map(mapper::toModel).toList();
    }

    @Override
    public PageResult<User> getPage(int page, int size) {
        PanacheQuery<UserEntity> query = findAll(Sort.by("id"));
        long total = query.count();
        List<User> content =
                query.page(Page.of(page, size)).list().stream().map(mapper::toModel).toList();
        return new PageResult<>(content, total);
    }
}

package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.Order;
import dev.nipponten.domain.models.PageResult;
import dev.nipponten.domain.repositories.OrderRepository;
import dev.nipponten.infrastructure.entities.OrderEntity;
import dev.nipponten.infrastructure.mappers.OrderMapper;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class PanacheOrderRepository implements OrderRepository, PanacheRepository<OrderEntity> {

    @Inject OrderMapper mapper;

    @Override
    @Transactional
    public Order save(Order model) {
        OrderEntity entity;
        if (model.id() == null) {
            entity = mapper.toEntity(model);
            persist(entity);
        } else {
            entity = findById(model.id());
            mapper.updateEntity(entity, model);
        }
        // Gera os ids dos itens antes de devolver o modelo.
        flush();
        return mapper.toModel(entity);
    }

    @Override
    public Order getById(Long id) {
        OrderEntity entity = findById(id);
        return entity == null ? null : mapper.toModel(entity);
    }

    @Override
    public boolean existsByCode(String code) {
        return count("code", code) > 0;
    }

    // Mais recentes primeiro: é o histórico do cliente. O id desempata e mantém as páginas
    // estáveis.
    @Override
    public PageResult<Order> getPageByClient(
            Long clientId, Set<Order.Status> statuses, int page, int size) {
        List<String> where = new ArrayList<>(List.of("clientId = :clientId"));
        Map<String, Object> params = new HashMap<>(Map.of("clientId", clientId));
        addStatuses(where, params, statuses);
        return toPage(
                find(
                        String.join(" and ", where),
                        Sort.by("createdAt").descending().and("id").descending(),
                        params),
                page,
                size);
    }

    @Override
    public PageResult<Order> getPage(Set<Order.Status> statuses, String code, int page, int size) {
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        addStatuses(where, params, statuses);
        if (code != null) {
            where.add("code = :code");
            params.put("code", code);
        }
        return toPage(
                where.isEmpty()
                        ? findAll(Sort.by("id"))
                        : find(String.join(" and ", where), Sort.by("id"), params),
                page,
                size);
    }

    private static void addStatuses(
            List<String> where, Map<String, Object> params, Set<Order.Status> statuses) {
        if (statuses.isEmpty()) {
            return;
        }
        where.add("status in :statuses");
        params.put(
                "statuses",
                statuses.stream()
                        .map(status -> OrderEntity.Status.valueOf(status.name()))
                        .toList());
    }

    private PageResult<Order> toPage(PanacheQuery<OrderEntity> query, int page, int size) {
        long total = query.count();
        List<Order> content =
                query.page(Page.of(page, size)).list().stream().map(mapper::toModel).toList();
        return new PageResult<>(content, total);
    }
}

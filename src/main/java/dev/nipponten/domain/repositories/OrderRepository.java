package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.Order;
import dev.nipponten.domain.models.PageResult;

public interface OrderRepository {
    // Grava o pedido com os itens e os filhos dos itens.
    Order save(Order model);

    Order getById(Long id);

    boolean existsByCode(String code);

    PageResult<Order> getPageByClient(Long clientId, int page, int size);
}

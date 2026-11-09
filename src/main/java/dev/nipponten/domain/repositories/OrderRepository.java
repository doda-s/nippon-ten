package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.Order;
import dev.nipponten.domain.models.PageResult;
import java.util.Set;

public interface OrderRepository {
    // Grava o pedido com os itens e os filhos dos itens.
    Order save(Order model);

    Order getById(Long id);

    boolean existsByCode(String code);

    // statuses vazio = todos os status.
    PageResult<Order> getPageByClient(
            Long clientId, Set<Order.Status> statuses, int page, int size);

    // statuses vazio = todos os status; code nulo = qualquer código.
    PageResult<Order> getPage(Set<Order.Status> statuses, String code, int page, int size);
}

package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.Order;
import dev.nipponten.domain.models.PageResult;
import java.time.Instant;
import java.util.List;

public interface OrderRepository {
    // Grava o pedido com os itens e os filhos dos itens.
    Order save(Order model);

    Order getById(Long id);

    boolean existsByCode(String code);

    /** Nulo se o código não existe. */
    Order getByCode(String code);

    // Troca só o hash do token, sem mexer no resto do pedido nem em updatedAt.
    void updateTrackingTokenHash(Long id, String trackingTokenHash);

    PageResult<Order> getPageByClient(Long clientId, int page, int size);

    PageResult<Order> getPage(int page, int size);

    PageResult<Order> getPageByStatus(Order.Status status, int page, int size);

    // Para a reconciliação agendada: no máximo {@code limit}, mais antigos primeiro.
    List<Order> getByStatusCreatedBefore(Order.Status status, Instant cutoff, int limit);
}

package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.Order;
import dev.nipponten.domain.models.PageResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface OrderRepository {
    // Grava o pedido com os itens e os filhos dos itens.
    Order save(Order model);

    Order getById(Long id);

    boolean existsByCode(String code);

    PageResult<Order> getPageByClient(Long clientId, int page, int size);

    PageResult<Order> getPage(int page, int size);

    PageResult<Order> getPageByStatus(Order.Status status, int page, int size);

    // Valor pago em dinheiro (total sem a taxa de entrega) de cada pedido COMPLETED de cliente
    // criado no intervalo [from, to).
    List<BigDecimal> getCompletedClientAmountsBetween(Instant from, Instant to);

    // Pontos usados em pedidos não cancelados criados no intervalo [from, to).
    long sumPointsRedeemedBetween(Instant from, Instant to);
}

package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.Order;
import dev.nipponten.domain.models.PageResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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

    // Valor pago em dinheiro (total sem a taxa de entrega) de cada pedido COMPLETED de cliente
    // criado no intervalo [from, to).
    List<BigDecimal> getCompletedClientAmountsBetween(Instant from, Instant to);

    // Pontos usados em pedidos não cancelados criados no intervalo [from, to).
    long sumPointsRedeemedBetween(Instant from, Instant to);
}

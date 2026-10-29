package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.OrderStatusChange;
import java.util.List;

public interface OrderStatusHistoryRepository {
    OrderStatusChange save(OrderStatusChange model);

    List<OrderStatusChange> getByOrder(Long orderId);
}

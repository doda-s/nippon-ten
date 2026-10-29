package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.OrderFeedback;

public interface OrderFeedbackRepository {
    OrderFeedback save(OrderFeedback model);

    // Nulo quando o pedido ainda não foi avaliado.
    OrderFeedback getByOrder(Long orderId);
}

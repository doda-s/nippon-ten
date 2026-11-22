package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.Payment;
import java.time.Instant;
import java.util.List;

public interface PaymentRepository {
    Payment save(Payment model);

    // Nulo quando o gateway avisa de um pagamento que não é deste sistema.
    Payment getByExternalId(String externalId);

    // Mais antigas primeiro.
    List<Payment> getByOrder(Long orderId);

    // Usados pela reconciliação agendada, com no máximo {@code limit} itens por vez, mais antigos
    // primeiro.
    List<Payment> getPendingExpiredBefore(Instant cutoff, int limit);

    List<Payment> getPendingWithoutExternalIdCreatedBefore(Instant cutoff, int limit);

    // Pagamentos aprovados de pedidos cancelados: o estorno ainda não aconteceu.
    List<Payment> getApprovedOfCancelledOrders(int limit);
}

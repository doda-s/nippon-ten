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

    // Tentativas abertas que já chegaram ao gateway, criadas antes de {@code createdCutoff} e ainda
    // no prazo ({@code expiresAt} nulo ou não anterior a {@code expiresCutoff}). Mais novas
    // primeiro: é nelas que o cliente está esperando.
    List<Payment> getPendingSentCreatedBefore(
            Instant createdCutoff, Instant expiresCutoff, int limit);

    // Pagamentos aprovados de pedidos cancelados: o estorno ainda não aconteceu.
    List<Payment> getApprovedOfCancelledOrders(int limit);
}

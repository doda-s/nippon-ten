package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.PageResult;
import dev.nipponten.domain.models.PointsTransaction;
import java.time.Instant;
import java.util.List;

public interface PointsTransactionRepository {
    PointsTransaction save(PointsTransaction model);

    PointsTransaction getById(Long id);

    // Lotes com saldo e ainda válidos, do que vence primeiro ao que não vence.
    List<PointsTransaction> getOpenLots(Long clientId, Instant now);

    // Lotes com saldo cujo vencimento já passou.
    List<PointsTransaction> getDueLots(Long clientId, Instant now);

    List<PointsTransaction> getByOrder(Long orderId, PointsTransaction.Type type);

    PageResult<PointsTransaction> getPageByClient(Long clientId, int page, int size);

    // Soma do saldo ainda válido de todos os clientes.
    long sumOpen(Instant now);
}

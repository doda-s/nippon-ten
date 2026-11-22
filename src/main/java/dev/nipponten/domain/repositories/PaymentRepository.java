package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.Payment;
import java.util.List;

public interface PaymentRepository {
    Payment save(Payment model);

    // Nulo quando o gateway avisa de um pagamento que não é deste sistema.
    Payment getByExternalId(String externalId);

    // Mais antigas primeiro.
    List<Payment> getByOrder(Long orderId);
}

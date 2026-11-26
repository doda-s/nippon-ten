package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.DeliverySettings;

public interface DeliverySettingsRepository {
    // Sempre existe: a migration cria a linha única.
    DeliverySettings get();

    DeliverySettings save(DeliverySettings model);
}

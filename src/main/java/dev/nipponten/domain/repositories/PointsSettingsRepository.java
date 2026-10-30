package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.PointsSettings;

public interface PointsSettingsRepository {
    // Sempre existe: a migration cria a linha única.
    PointsSettings get();

    PointsSettings save(PointsSettings model);
}

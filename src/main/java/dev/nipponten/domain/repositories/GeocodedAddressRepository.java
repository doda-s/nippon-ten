package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.GeocodedAddress;
import java.time.Instant;

public interface GeocodedAddressRepository {

    /** Nulo se não está no cache. */
    GeocodedAddress getByHash(String addressHash);

    /** Insere ou substitui. */
    void save(GeocodedAddress model);

    /** Apaga o que foi geocodificado antes de cutoff e devolve quantas linhas saíram. */
    int removeGeocodedBefore(Instant cutoff);
}

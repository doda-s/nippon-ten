package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.Coordinates;
import dev.nipponten.domain.models.GeocodedAddress;
import dev.nipponten.domain.repositories.GeocodedAddressRepository;
import dev.nipponten.infrastructure.entities.GeocodedAddressEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import org.hibernate.query.NativeQuery;

@ApplicationScoped
public class PanacheGeocodedAddressRepository
        implements GeocodedAddressRepository, PanacheRepositoryBase<GeocodedAddressEntity, String> {

    @Override
    public GeocodedAddress getByHash(String addressHash) {
        GeocodedAddressEntity entity = findById(addressHash);
        if (entity == null) {
            return null;
        }
        Coordinates coordinates =
                entity.getLatitude() == null || entity.getLongitude() == null
                        ? null
                        : new Coordinates(entity.getLatitude(), entity.getLongitude());
        return new GeocodedAddress(entity.getAddressHash(), coordinates, entity.getGeocodedAt());
    }

    // Upsert nativo: a cotação e o checkout do mesmo endereço podem chegar juntos. O tipo de cada
    // parâmetro vai explícito porque o nulo sem tipo chega ao Postgres como bytea.
    @Override
    @Transactional
    public void save(GeocodedAddress model) {
        Coordinates coordinates = model.coordinates();
        getEntityManager()
                .createNativeQuery(
                        "insert into geocoded_address (address_hash, latitude, longitude,"
                                + " geocoded_at) values (?1, ?2, ?3, ?4) on conflict"
                                + " (address_hash) do update set latitude = excluded.latitude,"
                                + " longitude = excluded.longitude, geocoded_at ="
                                + " excluded.geocoded_at")
                .unwrap(NativeQuery.class)
                .setParameter(1, model.addressHash(), String.class)
                .setParameter(2, coordinates == null ? null : coordinates.latitude(), Double.class)
                .setParameter(3, coordinates == null ? null : coordinates.longitude(), Double.class)
                .setParameter(4, model.geocodedAt(), Instant.class)
                .executeUpdate();
    }

    @Override
    @Transactional
    public int removeGeocodedBefore(Instant cutoff) {
        return (int) delete("geocodedAt < ?1", cutoff);
    }
}

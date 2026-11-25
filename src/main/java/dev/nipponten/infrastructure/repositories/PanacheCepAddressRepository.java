package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.CepAddress;
import dev.nipponten.domain.repositories.CepAddressRepository;
import dev.nipponten.infrastructure.entities.CepAddressEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import org.hibernate.query.NativeQuery;

@ApplicationScoped
public class PanacheCepAddressRepository
        implements CepAddressRepository, PanacheRepositoryBase<CepAddressEntity, String> {

    @Override
    public CepAddress getByCep(String cep) {
        CepAddressEntity entity = findById(cep);
        return entity == null
                ? null
                : new CepAddress(
                        entity.getCep(),
                        entity.getStreet(),
                        entity.getNeighborhood(),
                        entity.getCity(),
                        entity.getState(),
                        entity.getFetchedAt());
    }

    // Upsert nativo: duas consultas simultâneas do mesmo CEP não podem falhar na chave primária. O
    // tipo de cada parâmetro vai explícito porque o nulo sem tipo chega ao Postgres como bytea.
    @Override
    @Transactional
    public void save(CepAddress model) {
        getEntityManager()
                .createNativeQuery(
                        "insert into cep_address (cep, street, neighborhood, city, state,"
                                + " fetched_at) values (?1, ?2, ?3, ?4, ?5, ?6) on conflict (cep)"
                                + " do update set street = excluded.street, neighborhood ="
                                + " excluded.neighborhood, city = excluded.city, state ="
                                + " excluded.state, fetched_at = excluded.fetched_at")
                .unwrap(NativeQuery.class)
                .setParameter(1, model.cep(), String.class)
                .setParameter(2, model.street(), String.class)
                .setParameter(3, model.neighborhood(), String.class)
                .setParameter(4, model.city(), String.class)
                .setParameter(5, model.state(), String.class)
                .setParameter(6, model.fetchedAt(), Instant.class)
                .executeUpdate();
    }
}

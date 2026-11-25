package dev.nipponten.application.services;

import dev.nipponten.domain.gateways.Geocoder;
import dev.nipponten.domain.models.Address;
import dev.nipponten.domain.models.Coordinates;
import dev.nipponten.domain.models.GeocodedAddress;
import dev.nipponten.domain.repositories.GeocodedAddressRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Coordenada de um endereço, com cache. A cotação e o checkout do mesmo endereço fazem uma chamada
 * só ao provedor. Os termos do Google permitem guardar lat/lng por no máximo 30 dias: o TTL fica
 * abaixo disso, e o GeocodeCachePurgeJob apaga as linhas vencidas.
 */
@ApplicationScoped
public class GeocodingService {

    @Inject Geocoder geocoder;

    @Inject GeocodedAddressRepository cache;

    @Inject Clock clock;

    @ConfigProperty(name = "nippon.delivery.geocode-cache-ttl")
    Duration cacheTtl;

    /** O endereço precisa de cidade e UF (ver {@link CepService#complete}). */
    public Optional<Coordinates> locate(Address address) {
        String hash = hash(address);
        Instant now = Instant.now(clock);
        GeocodedAddress cached = cache.getByHash(hash);
        if (cached != null && cached.geocodedAt().plus(cacheTtl).isAfter(now)) {
            return Optional.ofNullable(cached.coordinates());
        }
        Optional<Coordinates> found = geocoder.geocode(address);
        // O "não achou" também fica em cache, para a cotação repetida não chamar o provedor de
        // novo.
        cache.save(new GeocodedAddress(hash, found.orElse(null), now));
        return found;
    }

    /** Apaga as coordenadas vencidas e devolve quantas saíram. */
    public int purgeExpired() {
        return cache.removeGeocodedBefore(Instant.now(clock).minus(cacheTtl));
    }

    // O complemento não muda a coordenada, e o CEP fica de fora porque o geocoder não o usa.
    static String hash(Address address) {
        String key =
                String.join(
                        "|",
                        Stream.of(
                                        address.streetAddress(),
                                        address.number(),
                                        address.neighborhood(),
                                        address.city(),
                                        address.state())
                                .map(GeocodingService::normalize)
                                .toList());
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(key.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}

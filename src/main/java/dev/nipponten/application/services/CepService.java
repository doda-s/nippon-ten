package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.AddressLookupException;
import dev.nipponten.application.exceptions.CepNotFoundException;
import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.domain.gateways.CepLookup;
import dev.nipponten.domain.models.Address;
import dev.nipponten.domain.models.CepAddress;
import dev.nipponten.domain.repositories.CepAddressRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Consulta de CEP com cache no banco. O CEP muda pouco, então o cache evita chamar o provedor a
 * cada cotação; vencido, ele é consultado de novo, e se o provedor estiver fora do ar vale a cópia
 * antiga.
 */
@ApplicationScoped
public class CepService {

    @Inject CepLookup lookup;

    @Inject CepAddressRepository cache;

    @Inject Clock clock;

    @ConfigProperty(name = "nippon.delivery.cep-cache-ttl")
    Duration cacheTtl;

    /** Só os 8 dígitos, que é como o CEP vai para o provedor e para o cache. */
    public static String digits(String cep) {
        return cep == null ? "" : cep.replaceAll("\\D", "");
    }

    public Optional<CepAddress> find(String cep) {
        String key = digits(cep);
        if (key.length() != 8) {
            return Optional.empty();
        }
        CepAddress cached = cache.getByCep(key);
        if (cached != null && cached.fetchedAt().plus(cacheTtl).isAfter(Instant.now(clock))) {
            return Optional.of(cached);
        }
        try {
            Optional<CepAddress> found = lookup.find(key);
            found.ifPresent(cache::save);
            return found;
        } catch (AddressLookupException e) {
            if (cached != null) {
                return Optional.of(cached);
            }
            throw e;
        }
    }

    public CepAddress getByCep(String cep) {
        return find(cep).orElseThrow(() -> new CepNotFoundException(cep));
    }

    /**
     * Completa o endereço digitado com bairro, cidade e UF do CEP. CEP inexistente é 400: ele veio
     * no corpo, junto do resto do endereço.
     */
    public Address complete(Address address) {
        CepAddress found =
                find(address.cep())
                        .orElseThrow(
                                () ->
                                        new InvalidRequestException(
                                                "cep " + address.cep() + " was not found"));
        return new Address(
                address.streetAddress(),
                address.number(),
                address.complement(),
                address.cep(),
                found.neighborhood() == null ? address.neighborhood() : found.neighborhood(),
                found.city(),
                found.state());
    }
}

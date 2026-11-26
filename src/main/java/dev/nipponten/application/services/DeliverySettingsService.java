package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.AddressLookupException;
import dev.nipponten.application.exceptions.GeocodingException;
import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.domain.models.Address;
import dev.nipponten.domain.models.Coordinates;
import dev.nipponten.domain.models.DeliverySettings;
import dev.nipponten.domain.models.LocatedAddress;
import dev.nipponten.domain.repositories.DeliverySettingsRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jboss.logging.Logger;

/**
 * A loja é cadastrada pelo endereço, e o pino no mapa corrige a coordenada quando o geocoder erra
 * ou não acha. A coordenada do endereço não é gravada aqui: ela vem do GeocodingService, cujo cache
 * respeita o limite de 30 dias dos termos do Google.
 */
@ApplicationScoped
public class DeliverySettingsService {

    private static final Logger LOG = Logger.getLogger(DeliverySettingsService.class);

    @Inject DeliverySettingsRepository repository;

    @Inject CepService cepService;

    @Inject GeocodingService geocodingService;

    @Inject CurrentUser currentUser;

    @Inject Clock clock;

    public DeliverySettings get() {
        return repository.get();
    }

    // Vale só para pedidos novos: cada pedido guarda a taxa e a distância do próprio checkout.
    public DeliverySettings update(DeliverySettings model) {
        // Duas casas, como no banco: a resposta do PUT sai igual à do GET (5.00, não 5), e 3, 3.0 e
        // 3.00 viram a mesma faixa. O request já limita a 2 casas, então não há arredondamento.
        List<DeliverySettings.Tier> tiers =
                model.tiers().stream()
                        .map(
                                tier ->
                                        new DeliverySettings.Tier(
                                                tier.maxDistanceKm().setScale(2),
                                                tier.fee().setScale(2)))
                        .sorted(Comparator.comparing(DeliverySettings.Tier::maxDistanceKm))
                        .toList();
        Set<BigDecimal> distances = new HashSet<>();
        for (DeliverySettings.Tier tier : tiers) {
            if (!distances.add(tier.maxDistanceKm())) {
                throw new InvalidRequestException(
                        "Two tiers share maxDistanceKm " + tier.maxDistanceKm());
            }
        }
        LocatedAddress store = locate(model.storeAddress());
        // Com o pino, o endereço não precisa ser achado: o pino existe para esse caso.
        if (model.pinnedLocation() == null && store.coordinates() == null) {
            throw new InvalidRequestException(
                    "The store address could not be located: check the street, number and cep, or"
                            + " place the pin on the map");
        }
        return repository.save(
                new DeliverySettings(
                        store.address(),
                        model.pinnedLocation(),
                        tiers,
                        Instant.now(clock),
                        currentUser.get().userId()));
    }

    /**
     * Completa o endereço da loja pelo CEP e busca a coordenada dele, para o front pôr o pino antes
     * de salvar. CEP inexistente é 400; provedor fora do ar é 502.
     */
    public LocatedAddress locate(Address address) {
        Address completed = cepService.complete(address);
        try {
            return new LocatedAddress(completed, geocodingService.locate(completed).orElse(null));
        } catch (GeocodingException e) {
            throw new AddressLookupException("Store geocoding failed", e);
        }
    }

    /**
     * De onde a distância é medida: o pino ou, sem ele, a coordenada do endereço. Vazio com o
     * cálculo desligado, ou se o endereço deixou de ser achado. Lança GeocodingException se o
     * provedor falhar.
     */
    public Optional<Coordinates> storeLocation(DeliverySettings settings) {
        if (settings.pinnedLocation() != null) {
            return Optional.of(settings.pinnedLocation());
        }
        if (settings.storeAddress() == null) {
            return Optional.empty();
        }
        return geocodingService.locate(settings.storeAddress());
    }

    /** Para mostrar a loja no mapa: com o provedor fora do ar, a leitura sai sem a coordenada. */
    public Coordinates storeLocationOrNull(DeliverySettings settings) {
        try {
            return storeLocation(settings).orElse(null);
        } catch (GeocodingException e) {
            LOG.warn("Store geocoding failure", e);
            return null;
        }
    }
}

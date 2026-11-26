package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.GeocodingException;
import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.domain.models.Address;
import dev.nipponten.domain.models.Coordinates;
import dev.nipponten.domain.models.DeliveryQuote;
import dev.nipponten.domain.models.DeliverySettings;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.jboss.logging.Logger;

/**
 * Taxa de entrega pela distância em linha reta da loja até o endereço, nas faixas da configuração
 * (docs/delivery.md). Se não der para calcular, a entrega é recusada com 400 em vez de sair com uma
 * taxa errada: o cliente ainda pode escolher retirada.
 */
@ApplicationScoped
public class DeliveryFeeService {

    private static final Logger LOG = Logger.getLogger(DeliveryFeeService.class);

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    // Raio médio da Terra (IUGG).
    private static final double EARTH_RADIUS_KM = 6371.0088;

    @Inject DeliverySettingsService settingsService;

    @Inject GeocodingService geocodingService;

    /** O endereço precisa de cidade e UF (ver {@link CepService#complete}). */
    public DeliveryQuote quote(Address address) {
        DeliverySettings settings = settingsService.get();
        // Sem a loja configurada, a taxa continua 0, como na v1.
        if (!settings.enabled()) {
            return new DeliveryQuote(null, ZERO);
        }
        Coordinates store;
        Coordinates destination;
        try {
            // O endereço da loja foi achado ao salvar; se deixou de ser, é falha nossa, não do
            // cliente.
            store =
                    settingsService
                            .storeLocation(settings)
                            .orElseThrow(
                                    () -> new GeocodingException("The store could not be located"));
            destination =
                    geocodingService
                            .locate(address)
                            .orElseThrow(
                                    () ->
                                            new InvalidRequestException(
                                                    "The address could not be located for"
                                                            + " delivery: check the street, number"
                                                            + " and cep"));
        } catch (GeocodingException e) {
            LOG.warn("Geocoding failure", e);
            throw new InvalidRequestException(
                    "The delivery fee could not be calculated now: try again or choose PICKUP");
        }
        BigDecimal distance = distanceKm(store, destination);
        return settings.tiers().stream()
                .filter(tier -> distance.compareTo(tier.maxDistanceKm()) <= 0)
                .findFirst()
                .map(tier -> new DeliveryQuote(distance, tier.fee().setScale(2)))
                .orElseThrow(
                        () ->
                                new InvalidRequestException(
                                        "The address is "
                                                + distance
                                                + " km away and the delivery reaches "
                                                + settings.tiers().getLast().maxDistanceKm()
                                                + " km"));
    }

    // Haversine, arredondada para 2 casas (10 m).
    static BigDecimal distanceKm(Coordinates from, Coordinates to) {
        double lat1 = Math.toRadians(from.latitude());
        double lat2 = Math.toRadians(to.latitude());
        double deltaLat = lat2 - lat1;
        double deltaLng = Math.toRadians(to.longitude() - from.longitude());
        double a =
                Math.pow(Math.sin(deltaLat / 2), 2)
                        + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(deltaLng / 2), 2);
        double km = 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
        return BigDecimal.valueOf(km).setScale(2, RoundingMode.HALF_UP);
    }
}

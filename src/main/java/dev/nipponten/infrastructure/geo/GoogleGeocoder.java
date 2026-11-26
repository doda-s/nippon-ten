package dev.nipponten.infrastructure.geo;

import dev.nipponten.application.exceptions.GeocodingException;
import dev.nipponten.domain.gateways.Geocoder;
import dev.nipponten.domain.models.Address;
import dev.nipponten.domain.models.Coordinates;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Adapter do Google Geocoding. O CEP não entra na busca: a base de CEPs do Google no Brasil é
 * incompleta, e com ele no texto o Google chegou a devolver uma rua de mesmo nome em outra cidade.
 * A busca vai com rua, número e bairro, filtrada por cidade e UF, e o resultado só vale se cair
 * nessa cidade com precisão de rua ou melhor.
 */
@ApplicationScoped
public class GoogleGeocoder implements Geocoder {

    // APPROXIMATE é o centro do bairro ou da cidade: longe demais para cobrar por km.
    private static final List<String> PRECISE =
            List.of("ROOFTOP", "RANGE_INTERPOLATED", "GEOMETRIC_CENTER");

    @Inject @RestClient GoogleGeocodingClient client;

    // Opcional para a aplicação subir sem a chave (dev sem .env); a taxa de entrega vira 400.
    @ConfigProperty(name = "nippon.delivery.google.api-key")
    Optional<String> apiKey;

    @Override
    public Optional<Coordinates> geocode(Address address) {
        String key =
                apiKey.filter(value -> !value.isBlank())
                        .orElseThrow(
                                () ->
                                        new GeocodingException(
                                                "Google Geocoding API key is not set"));
        GoogleGeocodingClient.Response response;
        try {
            response =
                    client.geocode(
                            query(address),
                            "country:BR|administrative_area:"
                                    + address.state()
                                    + "|locality:"
                                    + address.city(),
                            "pt-BR",
                            "br",
                            key);
        } catch (WebApplicationException e) {
            throw new GeocodingException(
                    "Google Geocoding answered " + e.getResponse().getStatus(), e);
        } catch (ProcessingException e) {
            throw new GeocodingException("Google Geocoding is unreachable", e);
        }
        return switch (response.status()) {
            case "OK" ->
                    response.results().stream()
                            .filter(result -> PRECISE.contains(result.geometry().locationType()))
                            .filter(result -> isIn(result, address))
                            .findFirst()
                            .map(
                                    result ->
                                            new Coordinates(
                                                    result.geometry().location().lat(),
                                                    result.geometry().location().lng()));
            case "ZERO_RESULTS" -> Optional.empty();
            // OVER_QUERY_LIMIT, REQUEST_DENIED, INVALID_REQUEST, UNKNOWN_ERROR.
            default ->
                    throw new GeocodingException(
                            "Google Geocoding answered "
                                    + response.status()
                                    + ": "
                                    + response.errorMessage());
        };
    }

    private static String query(Address address) {
        StringBuilder query =
                new StringBuilder(address.streetAddress()).append(", ").append(address.number());
        if (address.neighborhood() != null) {
            query.append(", ").append(address.neighborhood());
        }
        return query.toString();
    }

    // O filtro de componentes restringe a busca, mas o Google ainda pode devolver outro lugar
    // parecido: confere UF e cidade no próprio resultado.
    private static boolean isIn(GoogleGeocodingClient.Result result, Address address) {
        List<GoogleGeocodingClient.Component> components =
                result.addressComponents() == null ? List.of() : result.addressComponents();
        boolean state =
                components.stream()
                        .anyMatch(
                                component ->
                                        component.types().contains("administrative_area_level_1")
                                                && address.state()
                                                        .equalsIgnoreCase(component.shortName()));
        boolean city =
                components.stream()
                        .anyMatch(
                                component ->
                                        (component.types().contains("locality")
                                                        || component
                                                                .types()
                                                                .contains(
                                                                        "administrative_area_level_2"))
                                                && normalize(address.city())
                                                        .equals(normalize(component.longName())));
        return state && city;
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}

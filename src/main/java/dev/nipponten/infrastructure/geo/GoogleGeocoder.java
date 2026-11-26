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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
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

    // Abreviações comuns nos nomes de rua, para "Av. Dr. Carlos Botelho" bater com "Avenida Doutor
    // Carlos Botelho" quando o Google devolve o nome por extenso.
    private static final Map<String, String> ABBREVIATIONS =
            Map.ofEntries(
                    Map.entry("r", "rua"),
                    Map.entry("av", "avenida"),
                    Map.entry("al", "alameda"),
                    Map.entry("tv", "travessa"),
                    Map.entry("trav", "travessa"),
                    Map.entry("pc", "praca"),
                    Map.entry("pca", "praca"),
                    Map.entry("rod", "rodovia"),
                    Map.entry("estr", "estrada"),
                    Map.entry("lgo", "largo"),
                    Map.entry("dr", "doutor"),
                    Map.entry("dra", "doutora"),
                    Map.entry("prof", "professor"),
                    Map.entry("profa", "professora"),
                    Map.entry("eng", "engenheiro"),
                    Map.entry("cel", "coronel"),
                    Map.entry("gen", "general"),
                    Map.entry("cap", "capitao"),
                    Map.entry("ten", "tenente"),
                    Map.entry("sgt", "sargento"),
                    Map.entry("mal", "marechal"),
                    Map.entry("alm", "almirante"),
                    Map.entry("sen", "senador"),
                    Map.entry("dep", "deputado"),
                    Map.entry("pres", "presidente"),
                    Map.entry("gov", "governador"),
                    Map.entry("pe", "padre"),
                    Map.entry("d", "dom"),
                    Map.entry("sta", "santa"),
                    Map.entry("sto", "santo"),
                    Map.entry("n", "nossa"),
                    Map.entry("sra", "senhora"));

    // O tipo do logradouro sai da comparação: o cliente pode digitar só "Paulista".
    private static final List<String> STREET_TYPES =
            List.of(
                    "rua",
                    "avenida",
                    "alameda",
                    "travessa",
                    "praca",
                    "rodovia",
                    "estrada",
                    "largo",
                    "viela",
                    "via",
                    "vila",
                    "passagem",
                    "ladeira",
                    "beco",
                    "servidao");

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
                            .filter(result -> isSameStreet(result, address))
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

    /**
     * Com partial_match, o Google não achou o endereço digitado e devolveu um parecido: uma rua
     * inventada chegou a voltar como um estacionamento em outra rua, com precisão ROOFTOP. O
     * resultado parcial só vale se a rua dele for a rua digitada, escrita de outro jeito
     * (abreviada, sem acento, sem o tipo do logradouro).
     */
    static boolean isSameStreet(GoogleGeocodingClient.Result result, Address address) {
        if (!result.partialMatch()) {
            return true;
        }
        List<GoogleGeocodingClient.Component> components =
                result.addressComponents() == null ? List.of() : result.addressComponents();
        return components.stream()
                .filter(component -> component.types().contains("route"))
                .flatMap(component -> Stream.of(component.longName(), component.shortName()))
                .anyMatch(route -> sameStreetName(address.streetAddress(), route));
    }

    /**
     * Iguais, ou um é o fim do outro em palavras inteiras ("Carlos Botelho" e "Doutor Carlos
     * Botelho").
     */
    static boolean sameStreetName(String typed, String found) {
        if (typed == null || found == null) {
            return false;
        }
        String a = streetName(typed);
        String b = streetName(found);
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        return a.equals(b) || a.endsWith(" " + b) || b.endsWith(" " + a);
    }

    private static String streetName(String value) {
        List<String> words =
                Arrays.stream(normalize(value).split("[^a-z0-9]+"))
                        .filter(word -> !word.isEmpty())
                        .map(word -> ABBREVIATIONS.getOrDefault(word, word))
                        .toList();
        if (words.size() > 1 && STREET_TYPES.contains(words.getFirst())) {
            words = words.subList(1, words.size());
        }
        return String.join(" ", words);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}

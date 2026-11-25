package dev.nipponten;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Imita o ViaCEP, a BrasilAPI e o Google Geocoding num servidor HTTP à parte, para os adapters
 * reais serem exercitados sem rede nem chave.
 *
 * <p>CEP: qualquer um existe, em São Carlos-SP, exceto {@link #CEP_UNKNOWN} (nenhum dos dois
 * conhece), {@link #CEP_ONLY_BRASILAPI} (só a BrasilAPI conhece), {@link #CEP_VIACEP_DOWN} (o
 * ViaCEP responde 500 e a BrasilAPI responde) e {@link #CEP_ALL_DOWN} (os dois respondem 500).
 *
 * <p>Google: a rua escolhe o resultado, medido a partir de {@link #STORE_LATITUDE}, {@link
 * #STORE_LONGITUDE}: {@code Rua Perto} (~0,6 km), {@code Rua Media} (~4 km), {@code Rua Longe} (~22
 * km), {@code Rua Inexistente} (ZERO_RESULTS), {@code Rua Vaga} (só o centro da cidade), {@code Rua
 * Outra Cidade} (resultado em outra cidade) e {@code Rua Cota} (OVER_QUERY_LIMIT). Qualquer outra
 * rua cai perto da loja.
 */
@ApplicationScoped
public class GeoStub {

    static final double STORE_LATITUDE = -22.0175;
    static final double STORE_LONGITUDE = -47.8908;

    static final String CEP_UNKNOWN = "99999999";
    static final String CEP_ONLY_BRASILAPI = "66666666";
    static final String CEP_VIACEP_DOWN = "88888888";
    static final String CEP_ALL_DOWN = "77777777";

    static final String KEY = "TEST-google-key";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Map<String, AtomicInteger> VIACEP_CALLS = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> GOOGLE_CALLS = new ConcurrentHashMap<>();

    @ConfigProperty(name = "nippon.test.geo-stub-port")
    int port;

    private HttpServer server;

    void start(@Observes StartupEvent event) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.createContext("/viacep/ws/", this::viaCep);
        server.createContext("/brasilapi/api/cep/v1/", this::brasilApi);
        server.createContext("/google/maps/api/geocode/json", this::google);
        server.start();
    }

    void stop(@Observes ShutdownEvent event) {
        server.stop(0);
    }

    static int viaCepCalls(String cep) {
        return VIACEP_CALLS.getOrDefault(cep, new AtomicInteger()).get();
    }

    /** Chamadas ao Google cuja busca contém {@code text} (a rua com o número, por exemplo). */
    static int googleCalls(String text) {
        return GOOGLE_CALLS.entrySet().stream()
                .filter(entry -> entry.getKey().contains(text))
                .mapToInt(entry -> entry.getValue().get())
                .sum();
    }

    // /viacep/ws/{cep}/json/
    private void viaCep(HttpExchange exchange) throws IOException {
        String cep = exchange.getRequestURI().getPath().split("/")[3];
        VIACEP_CALLS.computeIfAbsent(cep, key -> new AtomicInteger()).incrementAndGet();
        if (cep.equals(CEP_VIACEP_DOWN) || cep.equals(CEP_ALL_DOWN)) {
            send(exchange, 500, Map.of("message", "down"));
        } else if (cep.equals(CEP_UNKNOWN) || cep.equals(CEP_ONLY_BRASILAPI)) {
            send(exchange, 200, Map.of("erro", "true"));
        } else {
            send(
                    exchange,
                    200,
                    Map.of(
                            "cep", cep.substring(0, 5) + "-" + cep.substring(5),
                            "logradouro", "Rua do CEP",
                            "bairro", "Centro",
                            "localidade", "São Carlos",
                            "uf", "SP"));
        }
    }

    // /brasilapi/api/cep/v1/{cep}
    private void brasilApi(HttpExchange exchange) throws IOException {
        String cep = exchange.getRequestURI().getPath().split("/")[5];
        if (cep.equals(CEP_ALL_DOWN)) {
            send(exchange, 500, Map.of("message", "down"));
        } else if (cep.equals(CEP_UNKNOWN)) {
            send(exchange, 404, Map.of("message", "CEP não encontrado"));
        } else {
            send(
                    exchange,
                    200,
                    Map.of(
                            "cep", cep,
                            "street", "Rua da Reserva",
                            "neighborhood", "Vila Reserva",
                            "city", "São Carlos",
                            "state", "SP"));
        }
    }

    private void google(HttpExchange exchange) throws IOException {
        Map<String, String> query = query(exchange.getRequestURI().getRawQuery());
        String address = query.getOrDefault("address", "");
        GOOGLE_CALLS.computeIfAbsent(address, key -> new AtomicInteger()).incrementAndGet();
        if (!KEY.equals(query.get("key"))) {
            send(exchange, 200, Map.of("status", "REQUEST_DENIED", "results", List.of()));
            return;
        }
        Map<String, String> components = new HashMap<>();
        for (String component : query.getOrDefault("components", "").split("\\|")) {
            String[] parts = component.split(":", 2);
            if (parts.length == 2) {
                components.put(parts[0], parts[1]);
            }
        }
        String city = components.get("locality");
        String state = components.get("administrative_area");
        if (address.startsWith("Rua Inexistente")) {
            send(exchange, 200, Map.of("status", "ZERO_RESULTS", "results", List.of()));
        } else if (address.startsWith("Rua Cota")) {
            send(
                    exchange,
                    200,
                    Map.of(
                            "status",
                            "OVER_QUERY_LIMIT",
                            "error_message",
                            "quota",
                            "results",
                            List.of()));
        } else if (address.startsWith("Rua Vaga")) {
            send(
                    exchange,
                    200,
                    result(STORE_LATITUDE, STORE_LONGITUDE, "APPROXIMATE", city, state));
        } else if (address.startsWith("Rua Outra Cidade")) {
            send(exchange, 200, result(STORE_LATITUDE, STORE_LONGITUDE, "ROOFTOP", "Ibaté", state));
        } else if (address.startsWith("Rua Media")) {
            // 0,036° de latitude ≈ 4 km.
            send(
                    exchange,
                    200,
                    result(STORE_LATITUDE + 0.036, STORE_LONGITUDE, "ROOFTOP", city, state));
        } else if (address.startsWith("Rua Longe")) {
            send(
                    exchange,
                    200,
                    result(STORE_LATITUDE + 0.2, STORE_LONGITUDE, "ROOFTOP", city, state));
        } else {
            send(
                    exchange,
                    200,
                    result(
                            STORE_LATITUDE + 0.005,
                            STORE_LONGITUDE,
                            "RANGE_INTERPOLATED",
                            city,
                            state));
        }
    }

    private static Map<String, Object> result(
            double latitude, double longitude, String locationType, String city, String state) {
        return Map.of(
                "status",
                "OK",
                "results",
                List.of(
                        Map.of(
                                "address_components",
                                List.of(
                                        Map.of(
                                                "long_name", city,
                                                "short_name", city,
                                                "types", List.of("locality", "political")),
                                        Map.of(
                                                "long_name",
                                                "São Paulo",
                                                "short_name",
                                                state,
                                                "types",
                                                List.of(
                                                        "administrative_area_level_1",
                                                        "political"))),
                                "geometry",
                                Map.of(
                                        "location",
                                        Map.of("lat", latitude, "lng", longitude),
                                        "location_type",
                                        locationType))));
    }

    private static Map<String, String> query(String raw) {
        Map<String, String> params = new HashMap<>();
        if (raw == null) {
            return params;
        }
        for (String pair : raw.split("&")) {
            String[] parts = pair.split("=", 2);
            params.put(
                    URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "");
        }
        return params;
    }

    private static void send(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = JSON.writeValueAsBytes(body);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}

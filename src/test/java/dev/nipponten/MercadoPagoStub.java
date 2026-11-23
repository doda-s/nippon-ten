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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Imita a API Orders do Mercado Pago num servidor HTTP à parte, para o adapter real ser exercitado
 * sem rede nem credenciais. Fica fora da API da aplicação porque o "Authorization: Bearer" do
 * Mercado Pago seria lido como JWT nosso. Repete a order quando a chave de idempotência se repete,
 * como o Mercado Pago, e os testes mudam o status com {@link #setStatus}.
 *
 * <p>No cartão, o token escolhe o resultado, como o nome do titular no sandbox: {@code tok-APRO}
 * aprova, {@code tok-CONT} fica em processamento e {@code tok-OTHE} recusa com 402.
 */
@ApplicationScoped
public class MercadoPagoStub {

    static final String TOKEN = "Bearer TEST-access-token";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Map<String, Map<String, Object>> ORDERS = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> REQUESTS = new ConcurrentHashMap<>();
    private static final Map<String, String> BY_KEY = new ConcurrentHashMap<>();
    // Chaves de idempotência recebidas nos cancelamentos e estornos de cada order.
    private static final Map<String, List<String>> KEYS = new ConcurrentHashMap<>();
    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final AtomicInteger FAILURES = new AtomicInteger();

    @ConfigProperty(name = "nippon.test.mercadopago-stub-port")
    int port;

    private HttpServer server;

    void start(@Observes StartupEvent event) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.createContext("/v1/orders", this::handle);
        server.start();
    }

    void stop(@Observes ShutdownEvent event) {
        server.stop(0);
    }

    /** As próximas {@code count} criações respondem 500. */
    static void failNext(int count) {
        FAILURES.set(count);
    }

    static void setStatus(String id, String status, String detail) {
        Map<String, Object> order = ORDERS.get(id);
        order.put("status", status);
        order.put("status_detail", detail);
    }

    static Map<String, Object> requestOf(String id) {
        return REQUESTS.get(id);
    }

    static int created() {
        return SEQUENCE.get();
    }

    /** Chaves de idempotência dos cancelamentos e estornos recebidos para a order. */
    static List<String> operationKeysOf(String id) {
        return KEYS.getOrDefault(id, List.of());
    }

    /** Id da última order criada. Os testes rodam um por vez, então é a do teste atual. */
    static String lastId() {
        return "ORD01STUB" + SEQUENCE.get();
    }

    @SuppressWarnings("unchecked")
    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!TOKEN.equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                reply(exchange, 401, Map.of("message", "invalid token"));
                return;
            }
            String path = exchange.getRequestURI().getPath();
            if (exchange.getRequestMethod().equals("GET")) {
                Map<String, Object> order = ORDERS.get(path.substring("/v1/orders/".length()));
                reply(exchange, order == null ? 404 : 200, order == null ? Map.of() : order);
                return;
            }
            String key = exchange.getRequestHeaders().getFirst("X-Idempotency-Key");
            if (key == null || key.isBlank()) {
                reply(exchange, 400, Map.of("message", "missing idempotency"));
                return;
            }
            if (FAILURES.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
                reply(exchange, 500, Map.of("message", "internal_error"));
                return;
            }
            if (path.endsWith("/cancel") || path.endsWith("/refund")) {
                String id = path.substring("/v1/orders/".length(), path.lastIndexOf('/'));
                KEYS.computeIfAbsent(id, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                        .add(key);
                Map<String, Object> order = ORDERS.get(id);
                if (order == null) {
                    reply(
                            exchange,
                            404,
                            Map.of("errors", List.of(Map.of("code", "order_not_found"))));
                } else if (path.endsWith("/cancel")) {
                    cancel(exchange, order);
                } else {
                    refund(exchange, order);
                }
                return;
            }
            Map<String, Object> body = JSON.readValue(exchange.getRequestBody(), Map.class);
            String id = BY_KEY.computeIfAbsent(key, k -> newOrder(body));
            Map<String, Object> order = ORDERS.get(id);
            // A API responde 402 quando a order é criada, mas a transação falha, com a order em
            // "data" (formato visto no sandbox).
            if ("failed".equals(order.get("status"))) {
                reply(
                        exchange,
                        402,
                        Map.of(
                                "errors",
                                List.of(
                                        Map.of(
                                                "code",
                                                "failed",
                                                "message",
                                                "The following transactions failed")),
                                "data",
                                order));
            } else {
                reply(exchange, 201, order);
            }
        }
    }

    // Como a API: só cancela o que ainda não foi pago nem está em análise.
    private static void cancel(HttpExchange exchange, Map<String, Object> order)
            throws IOException {
        Object status = order.get("status");
        if ("canceled".equals(status)) {
            reply(
                    exchange,
                    409,
                    Map.of("errors", List.of(Map.of("code", "order_already_canceled"))));
        } else if ("action_required".equals(status) || "created".equals(status)) {
            order.put("status", "canceled");
            order.put("status_detail", "canceled_transaction");
            reply(exchange, 200, order);
        } else {
            reply(exchange, 409, Map.of("errors", List.of(Map.of("code", "cannot_cancel_order"))));
        }
    }

    // O estorno total mantém a order processed e muda o detalhe para refunded.
    private static void refund(HttpExchange exchange, Map<String, Object> order)
            throws IOException {
        if ("refunded".equals(order.get("status_detail"))) {
            reply(
                    exchange,
                    409,
                    Map.of("errors", List.of(Map.of("code", "order_already_refunded"))));
        } else if ("processed".equals(order.get("status"))) {
            order.put("status_detail", "refunded");
            reply(exchange, 201, order);
        } else {
            reply(exchange, 409, Map.of("errors", List.of(Map.of("code", "cannot_refund_order"))));
        }
    }

    private static void reply(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = JSON.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    // Ids em maiúsculas, como os reais (ORD...), para exercitar o minúsculo do manifesto.
    @SuppressWarnings("unchecked")
    private static String newOrder(Map<String, Object> body) {
        Map<String, Object> method =
                (Map<String, Object>)
                        ((List<Map<String, Object>>)
                                        ((Map<String, Object>) body.get("transactions"))
                                                .get("payments"))
                                .getFirst()
                                .get("payment_method");
        if ("credit_card".equals(method.get("type"))) {
            return newCardOrder(body, method);
        }
        String id = "ORD01STUB" + SEQUENCE.incrementAndGet();
        Map<String, Object> order = new HashMap<>();
        order.put("id", id);
        order.put("status", "action_required");
        order.put("status_detail", "waiting_transfer");
        order.put("total_amount", body.get("total_amount"));
        order.put(
                "transactions",
                Map.of(
                        "payments",
                        List.of(
                                Map.of(
                                        "id",
                                        "PAY01STUB" + id,
                                        "status",
                                        "action_required",
                                        "payment_method",
                                        Map.of(
                                                "id",
                                                "pix",
                                                "type",
                                                "bank_transfer",
                                                "qr_code",
                                                "00020126pix-" + id,
                                                "qr_code_base64",
                                                "iVBORw0KGgo-" + id)))));
        ORDERS.put(id, order);
        REQUESTS.put(id, body);
        return id;
    }

    private static String newCardOrder(Map<String, Object> body, Map<String, Object> method) {
        String id = "ORD01STUB" + SEQUENCE.incrementAndGet();
        String[] result =
                switch (String.valueOf(method.get("token"))) {
                    case "tok-APRO" -> new String[] {"processed", "accredited", "accredited"};
                    case "tok-CONT" -> new String[] {"processing", "in_process", "pending_review"};
                    default -> new String[] {"failed", "failed", "cc_rejected_insufficient_amount"};
                };
        Map<String, Object> order = new HashMap<>();
        order.put("id", id);
        order.put("status", result[0]);
        order.put("status_detail", result[1]);
        order.put("total_amount", body.get("total_amount"));
        order.put(
                "transactions",
                Map.of(
                        "payments",
                        List.of(
                                Map.of(
                                        "id",
                                        "PAY01STUB" + id,
                                        "status",
                                        result[0],
                                        "status_detail",
                                        result[2],
                                        "payment_method",
                                        Map.of("id", method.get("id"), "type", "credit_card")))));
        ORDERS.put(id, order);
        REQUESTS.put(id, body);
        return id;
    }
}

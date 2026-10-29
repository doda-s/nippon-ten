package dev.nipponten;

import static dev.nipponten.TestAuth.asAdmin;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OrderEventsTest {

    @TestHTTPResource("/orders")
    URL ordersUrl;

    private record Placed(long id, String token) {}

    private static Placed placeGuestOrder() {
        long size = TestFixtures.firstSizeOf(TestFixtures.createProduct("Pastel com aviso"));
        JsonPath created =
                given().contentType(ContentType.JSON)
                        .body(
                                Map.of(
                                        "items",
                                        List.of(
                                                Map.of(
                                                        "type",
                                                        "PRODUCT",
                                                        "productSizeId",
                                                        size,
                                                        "quantity",
                                                        1)),
                                        "fulfillmentType",
                                        "PICKUP",
                                        "paymentMethod",
                                        "CASH",
                                        "guestName",
                                        "Bia",
                                        "guestPhone",
                                        "11977776666"))
                        .post("/orders")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath();
        return new Placed(created.getLong("order.id"), created.getString("trackingToken"));
    }

    /**
     * Abre o SSE e devolve a primeira linha de dados que satisfaz o filtro. A conexão é aberta
     * antes de a ação acontecer; a espera curta garante que a assinatura já está ativa, porque o
     * stream não reenvia eventos passados.
     */
    private String firstEvent(
            String path, Map<String, String> headers, Runnable action, Predicate<String> filter)
            throws Exception {
        HttpRequest.Builder request =
                HttpRequest.newBuilder(URI.create(ordersUrl + path))
                        .header("Accept", "text/event-stream");
        headers.forEach(request::header);
        CompletableFuture<String> received =
                HttpClient.newHttpClient()
                        .sendAsync(request.build(), HttpResponse.BodyHandlers.ofLines())
                        .thenApply(
                                response -> {
                                    try (Stream<String> lines = response.body()) {
                                        return lines.filter(line -> line.startsWith("data:"))
                                                .filter(filter)
                                                .findFirst()
                                                .orElse("");
                                    }
                                });
        Thread.sleep(1000);
        action.run();
        return received.get(10, TimeUnit.SECONDS);
    }

    @Test
    void guestReceivesTheStatusChangeOfTheirOrder() throws Exception {
        Placed order = placeGuestOrder();

        String event =
                firstEvent(
                        "/" + order.id() + "/events",
                        Map.of("X-Order-Token", order.token()),
                        () ->
                                asAdmin()
                                        .contentType(ContentType.JSON)
                                        .body(Map.of("status", "IN_PREPARATION"))
                                        .post("/orders/{id}/status", order.id())
                                        .then()
                                        .statusCode(200),
                        line -> line.contains("STATUS_CHANGED"));

        assertTrue(event.contains("\"orderId\":" + order.id()), event);
        assertTrue(event.contains("\"status\":\"IN_PREPARATION\""), event);
    }

    @Test
    void dashboardReceivesNewOrders() throws Exception {
        long[] created = new long[1];

        String event =
                firstEvent(
                        "/events",
                        Map.of("Authorization", "Bearer " + TestAuth.adminToken()),
                        () -> created[0] = placeGuestOrder().id(),
                        line -> line.contains("CREATED"));

        assertTrue(event.contains("\"orderId\":" + created[0]), event);
    }

    @Test
    void orderStreamRequiresAccess() {
        Placed order = placeGuestOrder();

        given().get("/orders/{id}/events", order.id()).then().statusCode(401);
        given().header("X-Order-Token", "token-errado")
                .get("/orders/{id}/events", order.id())
                .then()
                .statusCode(403);
    }

    @Test
    void dashboardStreamRequiresManageOrders() {
        given().get("/orders/events").then().statusCode(401);
    }
}

package dev.nipponten;

import static dev.nipponten.TestAuth.as;
import static dev.nipponten.TestAuth.asAdmin;
import static dev.nipponten.TestAuth.login;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.nipponten.application.services.PaymentService;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PointsTest {

    private static final String PASSWORD = "senha-forte-123";

    // Configuração da migration; quem muda restaura no fim, porque o banco é compartilhado.
    private static final Map<String, Object> DEFAULT_SETTINGS =
            Map.of("earnRate", 1, "pointValue", 0.05, "expirationDays", 365);

    @Inject TestClock clock;

    @Inject PaymentService paymentService;

    private record Client(long userId, String token) {}

    private static Client newClient() {
        return newClient("cliente.pontos." + System.nanoTime() + "@example.com");
    }

    private static Client newClient(String email) {
        long userId =
                given().contentType(ContentType.JSON)
                        .body(
                                Map.of(
                                        "user", Map.of("email", email, "password", PASSWORD),
                                        "client",
                                                Map.of(
                                                        "name",
                                                        "Joao",
                                                        "lastName",
                                                        "Sumi",
                                                        "phone",
                                                        "11999990000")))
                        .post("/users")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("id");
        return new Client(userId, login(email, PASSWORD));
    }

    // Produto de R$10,00 no único tamanho.
    private static Map<String, Object> pastel(int quantity) {
        long product = TestFixtures.createProduct("Pastel dos pontos");
        return item(TestFixtures.firstSizeOf(product), quantity);
    }

    private static Map<String, Object> item(long productSizeId, int quantity) {
        return Map.of("type", "PRODUCT", "productSizeId", productSizeId, "quantity", quantity);
    }

    private static Map<String, Object> order(List<Map<String, Object>> items, Integer points) {
        Map<String, Object> body = new HashMap<>();
        body.put("items", items);
        body.put("fulfillmentType", "PICKUP");
        body.put("paymentMethod", "CASH");
        if (points != null) {
            body.put("pointsToRedeem", points);
        }
        return body;
    }

    private static ValidatableResponse place(Client client, Map<String, Object> body) {
        return as(client.token()).contentType(ContentType.JSON).body(body).post("/orders").then();
    }

    private static long placeOk(Client client, Map<String, Object> body) {
        return place(client, body).statusCode(201).extract().jsonPath().getLong("order.id");
    }

    private static ValidatableResponse complete(long orderId) {
        ValidatableResponse last = null;
        for (String status : List.of("IN_PREPARATION", "READY", "COMPLETED")) {
            last =
                    asAdmin()
                            .contentType(ContentType.JSON)
                            .body(Map.of("status", status))
                            .post("/orders/{id}/status", orderId)
                            .then()
                            .statusCode(200);
        }
        return last;
    }

    private static ValidatableResponse adjust(Client client, int points) {
        return asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("points", points, "reason", "Teste"))
                .post("/users/{id}/points/adjustments", client.userId())
                .then();
    }

    private static ValidatableResponse balanceOf(Client client) {
        return as(client.token()).get("/users/{id}/points", client.userId()).then().statusCode(200);
    }

    private static void putSettings(Map<String, Object> body) {
        asAdmin()
                .contentType(ContentType.JSON)
                .body(body)
                .put("/points/settings")
                .then()
                .statusCode(200);
    }

    @Test
    void earnsPointsWhenTheOrderIsCompleted() {
        Client client = newClient();
        long orderId = placeOk(client, order(List.of(pastel(2)), null));

        balanceOf(client).body("balance", is(0));
        complete(orderId).body("pointsEarned", is(20));

        balanceOf(client)
                .body("balance", is(20))
                .body("nextExpiration.points", is(20))
                .body("nextExpiration.expiresAt", notNullValue());
        as(client.token())
                .get("/users/{id}/client", client.userId())
                .then()
                .body("promotionPoints", is(20));
        as(client.token())
                .get("/users/{id}/points/transactions", client.userId())
                .then()
                .statusCode(200)
                .body("content[0].type", is("EARN"))
                .body("content[0].points", is(20))
                .body("content[0].orderId", is((int) orderId));
    }

    @Test
    void aCancelledOrderNeverEarns() {
        Client client = newClient();
        long orderId = placeOk(client, order(List.of(pastel(1)), null));

        as(client.token())
                .contentType(ContentType.JSON)
                .post("/orders/{id}/cancel", orderId)
                .then()
                .statusCode(200);

        balanceOf(client).body("balance", is(0));
    }

    @Test
    void redeemsPointsAsADiscount() {
        Client client = newClient();
        adjust(client, 100).statusCode(201).body("balance", is(100));
        List<Map<String, Object>> items = List.of(pastel(1));

        as(client.token())
                .contentType(ContentType.JSON)
                .body(Map.of("items", items, "pointsToRedeem", 100))
                .post("/orders/quote")
                .then()
                .statusCode(200)
                .body("discount", is(5.0f))
                .body("total", is(5.0f))
                .body("pointsRedeemed", is(100))
                .body("pointsToEarn", is(5));
        place(client, order(items, 100))
                .statusCode(201)
                .body("order.discount", is(5.0f))
                .body("order.total", is(5.0f))
                .body("order.pointsRedeemed", is(100));

        balanceOf(client).body("balance", is(0));
    }

    @Test
    void rejectsRedeemingMoreThanTheBalance() {
        Client client = newClient();
        adjust(client, 10).statusCode(201);
        List<Map<String, Object>> items = List.of(pastel(1));

        as(client.token())
                .contentType(ContentType.JSON)
                .body(Map.of("items", items, "pointsToRedeem", 11))
                .post("/orders/quote")
                .then()
                .statusCode(400);
        place(client, order(items, 11)).statusCode(400);
        balanceOf(client).body("balance", is(10));
    }

    @Test
    void rejectsADiscountAboveTheOrderValue() {
        Client client = newClient();
        adjust(client, 1000).statusCode(201);
        List<Map<String, Object>> items = List.of(pastel(1));

        // R$10,00 de itens = no máximo 200 pontos de R$0,05.
        place(client, order(items, 201)).statusCode(400);
        place(client, order(items, 200)).statusCode(201).body("order.total", is(0.0f));
        balanceOf(client).body("balance", is(800));
    }

    @Test
    void aPromotionThatRefusesPointsLeavesItsItemOutOfTheBase() {
        Client client = newClient();
        adjust(client, 300).statusCode(201);
        long product = TestFixtures.createProduct("Pastel promo sem pontos");
        Map<String, Object> promotion = new HashMap<>();
        promotion.put("title", "Promo");
        promotion.put("description", "d");
        promotion.put("status", "ACTIVE");
        promotion.put("promotionTypeId", TestFixtures.createPromotionType("10% sem pontos"));
        promotion.put("productId", product);
        promotion.put("startDate", "2020-01-01T00:00:00");
        promotion.put("endDate", "2099-12-31T00:00:00");
        promotion.put("enablePromotionPoints", false);
        asAdmin()
                .contentType(ContentType.JSON)
                .body(promotion)
                .post("/promotions")
                .then()
                .statusCode(201);
        Map<String, Object> promoted = item(TestFixtures.firstSizeOf(product), 1);

        as(client.token())
                .contentType(ContentType.JSON)
                .body(Map.of("items", List.of(promoted)))
                .post("/orders/quote")
                .then()
                .statusCode(200)
                .body("items[0].pointsEligible", is(false))
                .body("pointsEligibleSubtotal", is(0.0f));
        place(client, order(List.of(promoted), 1)).statusCode(400);
        // Com um item elegível, o desconto cobre só ele.
        place(client, order(List.of(promoted, pastel(1)), 201)).statusCode(400);
        place(client, order(List.of(promoted, pastel(1)), 100))
                .statusCode(201)
                .body("order.discount", is(5.0f));
    }

    @Test
    void guestsCannotRedeemPoints() {
        List<Map<String, Object>> items = List.of(pastel(1));
        Map<String, Object> body = order(items, 10);
        body.put("guestName", "Ana");
        body.put("guestPhone", "11988887777");

        given().contentType(ContentType.JSON).body(body).post("/orders").then().statusCode(400);
        given().contentType(ContentType.JSON)
                .body(Map.of("items", items, "pointsToRedeem", 10))
                .post("/orders/quote")
                .then()
                .statusCode(400);
    }

    @Test
    void cancellingGivesTheRedeemedPointsBack() {
        Client client = newClient();
        adjust(client, 100).statusCode(201);
        long orderId = placeOk(client, order(List.of(pastel(1)), 100));
        balanceOf(client).body("balance", is(0));

        as(client.token())
                .contentType(ContentType.JSON)
                .post("/orders/{id}/cancel", orderId)
                .then()
                .statusCode(200);

        balanceOf(client).body("balance", is(100));
        as(client.token())
                .get("/users/{id}/points/transactions", client.userId())
                .then()
                .body("content.type", hasItem("REFUND"));
    }

    @Test
    void anAbandonedOnlineOrderGivesTheRedeemedPointsBack() {
        Client client = newClient();
        adjust(client, 100).statusCode(201);
        Map<String, Object> body = order(List.of(pastel(1)), 100);
        body.put("paymentMethod", "PIX");
        body.put("paymentChannel", "ONLINE");
        long orderId = placeOk(client, body);
        balanceOf(client).body("balance", is(0));

        // Ninguém pagou: a reconciliação cancela o pedido e devolve os pontos.
        try {
            clock.advance(Duration.ofMinutes(31));
            paymentService.reconcile();
        } finally {
            clock.reset();
        }

        as(client.token()).get("/orders/{id}", orderId).then().body("status", is("CANCELLED"));
        balanceOf(client).body("balance", is(100));
    }

    @Test
    void anOrderWaitingForTheOnlinePaymentIsNotActive() {
        Client client = newClient();
        Map<String, Object> body = order(List.of(pastel(1)), null);
        body.put("paymentMethod", "PIX");
        body.put("paymentChannel", "ONLINE");
        long orderId = placeOk(client, body);

        as(client.token())
                .queryParam("active", true)
                .get("/users/{id}/orders", client.userId())
                .then()
                .statusCode(200)
                .body("content.id", not(hasItem((int) orderId)));
        as(client.token())
                .queryParam("status", "AWAITING_PAYMENT")
                .get("/users/{id}/orders", client.userId())
                .then()
                .body("content.id", hasItem((int) orderId));
    }

    @Test
    void editingBelowTheDiscountIsRejected() {
        Client client = newClient();
        adjust(client, 300).statusCode(201);
        Map<String, Object> twoPastels = pastel(2);
        long orderId = placeOk(client, order(List.of(twoPastels), 300));

        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("items", List.of(item((Long) twoPastels.get("productSizeId"), 1))))
                .put("/orders/{id}/items", orderId)
                .then()
                .statusCode(400);
        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("items", List.of(twoPastels, pastel(1))))
                .put("/orders/{id}/items", orderId)
                .then()
                .statusCode(200)
                .body("discount", is(15.0f))
                .body("total", is(15.0f));
    }

    @Test
    void pointsExpireAfterTheConfiguredDays() {
        String email = "cliente.expira." + System.nanoTime() + "@example.com";
        Client client = newClient(email);
        adjust(client, 50).statusCode(201);
        Map<String, Object> body = order(List.of(pastel(1)), 10);
        try {
            clock.advance(Duration.ofDays(366));
            // O token também segue o relógio da aplicação: o de antes já venceu.
            Client later = new Client(client.userId(), login(email, PASSWORD));

            balanceOf(later).body("balance", is(0)).body("nextExpiration", nullValue());
            as(later.token())
                    .get("/users/{id}/points/transactions", later.userId())
                    .then()
                    .body("content[0].type", is("EXPIRE"))
                    .body("content[0].points", is(-50));
            place(later, body).statusCode(400);
        } finally {
            clock.reset();
        }
    }

    @Test
    void staffAdjustsTheBalanceWithinLimits() {
        Client client = newClient();
        adjust(client, 30).statusCode(201).body("balance", is(30));

        adjust(client, -31).statusCode(400);
        adjust(client, 0).statusCode(400);
        adjust(client, -10).statusCode(201).body("balance", is(20));
        as(client.token())
                .contentType(ContentType.JSON)
                .body(Map.of("points", 10, "reason", "Eu mesmo"))
                .post("/users/{id}/points/adjustments", client.userId())
                .then()
                .statusCode(403);
        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("points", 10))
                .post("/users/{id}/points/adjustments", client.userId())
                .then()
                .statusCode(400);
    }

    @Test
    void onlyTheClientOrAManagerSeesTheStatement() {
        Client client = newClient();
        Client other = newClient();
        adjust(client, 5).statusCode(201);
        adjust(client, 5).statusCode(201);

        as(other.token()).get("/users/{id}/points", client.userId()).then().statusCode(403);
        as(other.token())
                .get("/users/{id}/points/transactions", client.userId())
                .then()
                .statusCode(403);
        asAdmin()
                .queryParam("size", 1)
                .get("/users/{id}/points/transactions", client.userId())
                .then()
                .statusCode(200)
                .body("totalElements", is(2))
                .body("totalPages", is(2))
                .body("content[0].reason", is("Teste"));
        given().get("/users/{id}/points", client.userId()).then().statusCode(401);
    }

    @Test
    void settingsChangeOnlyNewOrders() {
        Client client = newClient();
        long before = placeOk(client, order(List.of(pastel(1)), null));
        try {
            putSettings(Map.of("earnRate", 2, "pointValue", 0.10, "expirationDays", 30));
            given().get("/points/settings")
                    .then()
                    .statusCode(200)
                    .body("earnRate", is(2.0f))
                    .body("pointValue", is(0.1f))
                    .body("expirationDays", is(30));
            long after = placeOk(client, order(List.of(pastel(1)), null));

            complete(before).body("pointsEarned", is(10));
            complete(after).body("pointsEarned", is(20));
        } finally {
            putSettings(DEFAULT_SETTINGS);
        }
    }

    @Test
    void rejectsInvalidSettings() {
        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("earnRate", 1, "pointValue", 0))
                .put("/points/settings")
                .then()
                .statusCode(400);
        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("earnRate", -1, "pointValue", 0.05))
                .put("/points/settings")
                .then()
                .statusCode(400);
    }

    @Test
    void simulatesWhatAPurchaseEarns() {
        JsonPath result = simulate(Map.of("earnRate", 1.5, "pointValue", 0.20, "amount", 10.99));

        assertMoney("10.99", result, "amount");
        // Atual: 1 ponto por R$1 e R$0,05 por ponto.
        assertMoney("1", result, "current.earnRate");
        assertEquals(10, result.getInt("current.pointsEarned"));
        assertMoney("0.50", result, "current.pointsWorth");
        // Proposta: 10,99 × 1,5 = 16,485, arredondado para baixo como no crédito.
        assertEquals(16, result.getInt("proposed.pointsEarned"));
        assertMoney("3.20", result, "proposed.pointsWorth");

        JsonPath free = simulate(Map.of("earnRate", 0, "pointValue", 0.05, "amount", 50));
        assertEquals(0, free.getInt("proposed.pointsEarned"));
        assertMoney("0.00", free, "proposed.pointsWorth");
    }

    @Test
    void rejectsAnInvalidSimulation() {
        for (Map<String, Object> body :
                List.<Map<String, Object>>of(
                        Map.of("earnRate", 1, "pointValue", 0.05),
                        Map.of("earnRate", 1, "pointValue", 0.05, "amount", -1),
                        Map.of("earnRate", 1, "pointValue", 0, "amount", 10))) {
            asAdmin()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .post("/points/settings/simulation")
                    .then()
                    .statusCode(400);
        }
    }

    private static JsonPath simulate(Map<String, Object> body) {
        return asAdmin()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/points/settings/simulation")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();
    }

    private static void assertMoney(String expected, JsonPath json, String path) {
        assertEquals(
                0, new BigDecimal(expected).compareTo(new BigDecimal(json.getString(path))), path);
    }
}

package dev.nipponten;

import static dev.nipponten.TestAuth.as;
import static dev.nipponten.TestAuth.asAdmin;
import static dev.nipponten.TestAuth.login;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PaymentPixTest {

    private static final Map<String, Object> PIX = Map.of("method", "PIX");

    private record Placed(long id, String code, String token) {}

    private static Map<String, Object> orderBody(String method, String channel) {
        long size = TestFixtures.firstSizeOf(TestFixtures.createProduct("Pastel online"));
        Map<String, Object> body = new HashMap<>();
        body.put("items", List.of(Map.of("type", "PRODUCT", "productSizeId", size, "quantity", 2)));
        body.put("fulfillmentType", "PICKUP");
        body.put("paymentMethod", method);
        if (channel != null) body.put("paymentChannel", channel);
        body.put("guestName", "Ana");
        body.put("guestPhone", "11988887777");
        body.put("guestEmail", "ana@example.com");
        return body;
    }

    private static Placed placeGuest(String method, String channel) {
        JsonPath created =
                given().contentType(ContentType.JSON)
                        .body(orderBody(method, channel))
                        .post("/orders")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath();
        return new Placed(
                created.getLong("order.id"),
                created.getString("order.code"),
                created.getString("trackingToken"));
    }

    private static ValidatableResponse pay(Placed order, Map<String, Object> body) {
        return given().header("X-Order-Token", order.token())
                .contentType(ContentType.JSON)
                .body(body)
                .post("/orders/{id}/payments", order.id())
                .then();
    }

    private static ValidatableResponse getOrder(Placed order) {
        return given().header("X-Order-Token", order.token())
                .get("/orders/{id}", order.id())
                .then();
    }

    private static ValidatableResponse webhook(String dataId) throws Exception {
        return MercadoPagoWebhooks.send(dataId);
    }

    // Id externo da última tentativa, lido do stub pelo QR code que ele gerou.
    private static String externalIdOf(String qrCode) {
        return qrCode.substring("00020126pix-".length());
    }

    @Test
    void inPersonIsStillTheDefault() {
        Placed order = placeGuest("PIX", null);

        getOrder(order)
                .statusCode(200)
                .body("status", is("PENDING"))
                .body("paymentChannel", is("IN_PERSON"));
        pay(order, PIX).statusCode(400);
    }

    @Test
    void onlineOrdersWaitForThePayment() {
        Placed order = placeGuest("PIX", "ONLINE");

        getOrder(order)
                .statusCode(200)
                .body("status", is("AWAITING_PAYMENT"))
                .body("paymentChannel", is("ONLINE"))
                .body("statusHistory[0].status", is("AWAITING_PAYMENT"));
    }

    @Test
    void rejectsCashOnlineAndOnlineOrdersFromTheStaff() {
        given().contentType(ContentType.JSON)
                .body(orderBody("CASH", "ONLINE"))
                .post("/orders")
                .then()
                .statusCode(400);
        asAdmin()
                .contentType(ContentType.JSON)
                .body(orderBody("PIX", "ONLINE"))
                .post("/orders")
                .then()
                .statusCode(400);
    }

    @Test
    void guestPaysWithPixAndTheWebhookReleasesTheOrder() throws Exception {
        Placed order = placeGuest("PIX", "ONLINE");

        JsonPath payment =
                pay(order, PIX)
                        .statusCode(201)
                        .body("status", is("PENDING"))
                        .body("amount", notNullValue())
                        .body("pixQrCode", notNullValue())
                        .body("pixQrCodeBase64", notNullValue())
                        .body("expiresAt", notNullValue())
                        .extract()
                        .jsonPath();
        String externalId = externalIdOf(payment.getString("pixQrCode"));

        // O pagador é o e-mail que o visitante informou no pedido; o valor é o total do pedido.
        Map<String, Object> sent = MercadoPagoStub.requestOf(externalId);
        assertEquals("ana@example.com", ((Map<?, ?>) sent.get("payer")).get("email"));
        assertEquals("Ana", ((Map<?, ?>) sent.get("payer")).get("first_name"));
        assertEquals(
                0,
                new BigDecimal(getOrder(order).extract().jsonPath().getString("total"))
                        .compareTo(new BigDecimal((String) sent.get("total_amount"))));

        // Notificação sem pagamento ainda não muda nada.
        webhook(externalId).statusCode(200);
        getOrder(order).body("status", is("AWAITING_PAYMENT")).body("paid", is(false));

        MercadoPagoStub.setStatus(externalId, "processed", "accredited");
        webhook(externalId).statusCode(200);
        getOrder(order)
                .body("status", is("PENDING"))
                .body("paid", is(true))
                .body("paidAt", notNullValue())
                .body("statusHistory", hasSize(2));

        // O Mercado Pago reenvia: repetir não grava outra mudança.
        webhook(externalId).statusCode(200);
        getOrder(order).body("statusHistory", hasSize(2));
        given().header("X-Order-Token", order.token())
                .get("/orders/{id}/payments", order.id())
                .then()
                .statusCode(200)
                .body("", hasSize(1))
                .body("[0].status", is("APPROVED"))
                .body("[0].approvedAt", notNullValue());
    }

    @Test
    void clientPaysWithTheAccountEmail() {
        String email = "cliente.pix." + System.nanoTime() + "@example.com";
        given().contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "user", Map.of("email", email, "password", "senha-forte-123"),
                                "client", Map.of("name", "Joao", "lastName", "Sumi")))
                .post("/users")
                .then()
                .statusCode(201);
        String token = login(email, "senha-forte-123");
        Map<String, Object> body = orderBody("PIX", "ONLINE");
        body.remove("guestName");
        body.remove("guestPhone");
        body.remove("guestEmail");
        long orderId =
                as(token)
                        .contentType(ContentType.JSON)
                        .body(body)
                        .post("/orders")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("order.id");

        String qr =
                as(token)
                        .contentType(ContentType.JSON)
                        .body(PIX)
                        .post("/orders/{id}/payments", orderId)
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getString("pixQrCode");

        Map<?, ?> payer = (Map<?, ?>) MercadoPagoStub.requestOf(externalIdOf(qr)).get("payer");
        assertEquals(email, payer.get("email"));
        assertEquals("Joao", payer.get("first_name"));
    }

    @Test
    void onlyOneOpenPaymentAtATime() {
        Placed order = placeGuest("PIX", "ONLINE");

        pay(order, PIX).statusCode(201);
        pay(order, PIX).statusCode(400);
    }

    @Test
    void retriesAfterAGatewayFailureWithTheSameIdempotencyKey() {
        Placed order = placeGuest("PIX", "ONLINE");

        MercadoPagoStub.failNext(1);
        pay(order, PIX).statusCode(502);
        int before = MercadoPagoStub.created();
        pay(order, PIX).statusCode(201).body("status", is("PENDING"));

        assertEquals(before + 1, MercadoPagoStub.created());
        given().header("X-Order-Token", order.token())
                .get("/orders/{id}/payments", order.id())
                .then()
                .body("", hasSize(1));
    }

    @Test
    void anExpiredPixAllowsANewOne() throws Exception {
        Placed order = placeGuest("PIX", "ONLINE");
        String qr = pay(order, PIX).statusCode(201).extract().jsonPath().getString("pixQrCode");

        MercadoPagoStub.setStatus(externalIdOf(qr), "canceled", "expired");
        webhook(externalIdOf(qr)).statusCode(200);

        getOrder(order).body("status", is("AWAITING_PAYMENT"));
        pay(order, PIX).statusCode(201);
        given().header("X-Order-Token", order.token())
                .get("/orders/{id}/payments", order.id())
                .then()
                .body("[0].status", is("EXPIRED"))
                .body("[1].status", is("PENDING"));
    }

    @Test
    void refusesPaymentsThatDoNotMatchTheOrder() {
        Placed card = placeGuest("CARD", "ONLINE");
        pay(card, PIX).statusCode(400);
        // Cartão chega na próxima fase.
        pay(card, Map.of("method", "CARD")).statusCode(400);
    }

    @Test
    void onlyTheCustomerPaysButTheStaffSeesThePayments() {
        Placed order = placeGuest("PIX", "ONLINE");

        given().header("X-Order-Token", "token-errado")
                .contentType(ContentType.JSON)
                .body(PIX)
                .post("/orders/{id}/payments", order.id())
                .then()
                .statusCode(403);
        given().contentType(ContentType.JSON)
                .body(PIX)
                .post("/orders/{id}/payments", order.id())
                .then()
                .statusCode(401);
        asAdmin()
                .contentType(ContentType.JSON)
                .body(PIX)
                .post("/orders/{id}/payments", order.id())
                .then()
                .statusCode(403);

        pay(order, PIX).statusCode(201);
        asAdmin()
                .get("/orders/{id}/payments", order.id())
                .then()
                .statusCode(200)
                .body("", hasSize(1));
    }

    @Test
    void rejectsWebhooksWithoutAValidSignature() {
        given().queryParam("type", "order")
                .queryParam("data.id", "ORD01ANY")
                .header("x-request-id", "req-1")
                .header("x-signature", "ts=1742505638683,v1=deadbeef")
                .post("/payments/webhooks/mercadopago")
                .then()
                .statusCode(401);
        given().queryParam("type", "order")
                .queryParam("data.id", "ORD01ANY")
                .post("/payments/webhooks/mercadopago")
                .then()
                .statusCode(401);
    }

    @Test
    void ignoresWebhooksForUnknownOrders() throws Exception {
        webhook("ORD01NOTOURS").statusCode(200);
    }

    @Test
    void onlineOrdersFollowTheGatewayNotTheStaff() throws Exception {
        Placed order = placeGuest("PIX", "ONLINE");

        // Ninguém tira da espera nem marca como pago à mão.
        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("status", "PENDING"))
                .post("/orders/{id}/status", order.id())
                .then()
                .statusCode(400);
        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("status", "IN_PREPARATION"))
                .post("/orders/{id}/status", order.id())
                .then()
                .statusCode(400);
        asAdmin().post("/orders/{id}/pay", order.id()).then().statusCode(400);
        long size = TestFixtures.firstSizeOf(TestFixtures.createProduct("Pastel editado"));
        asAdmin()
                .contentType(ContentType.JSON)
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
                                                1))))
                .put("/orders/{id}/items", order.id())
                .then()
                .statusCode(400);

        // O visitante desiste antes de pagar.
        given().header("X-Order-Token", order.token())
                .contentType(ContentType.JSON)
                .post("/orders/{id}/cancel", order.id())
                .then()
                .statusCode(200)
                .body("status", is("CANCELLED"))
                .body("cancelReason", nullValue());
    }

    @Test
    void aPaidOnlineOrderCannotBeCancelledBeforeRefunds() throws Exception {
        Placed order = placeGuest("PIX", "ONLINE");
        String qr = pay(order, PIX).statusCode(201).extract().jsonPath().getString("pixQrCode");
        MercadoPagoStub.setStatus(externalIdOf(qr), "processed", "accredited");
        webhook(externalIdOf(qr)).statusCode(200);

        given().header("X-Order-Token", order.token())
                .contentType(ContentType.JSON)
                .post("/orders/{id}/cancel", order.id())
                .then()
                .statusCode(400);
        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("reason", "Cliente desistiu"))
                .post("/orders/{id}/cancel", order.id())
                .then()
                .statusCode(400);
    }
}

package dev.nipponten;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PaymentCardTest {

    private record Placed(long id, String token) {}

    private static Placed placeGuest(String method) {
        long size = TestFixtures.firstSizeOf(TestFixtures.createProduct("Pastel cartão"));
        Map<String, Object> body = new HashMap<>();
        body.put("items", List.of(Map.of("type", "PRODUCT", "productSizeId", size, "quantity", 1)));
        body.put("fulfillmentType", "PICKUP");
        body.put("paymentMethod", method);
        body.put("paymentChannel", "ONLINE");
        body.put("guestName", "Ana");
        body.put("guestPhone", "11988887777");
        body.put("guestEmail", "ana@example.com");
        JsonPath created =
                given().contentType(ContentType.JSON)
                        .body(body)
                        .post("/orders")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath();
        return new Placed(created.getLong("order.id"), created.getString("trackingToken"));
    }

    private static Map<String, Object> card(String token) {
        return Map.of("method", "CARD", "card", Map.of("token", token, "paymentMethodId", "visa"));
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

    @Test
    void approvedCardReleasesTheOrderRightAway() {
        Placed order = placeGuest("CARD");

        pay(
                        order,
                        Map.of(
                                "method",
                                "CARD",
                                "card",
                                Map.of(
                                        "token",
                                        "tok-APRO",
                                        "paymentMethodId",
                                        "master",
                                        "payerEmail",
                                        "titular@example.com",
                                        "identification",
                                        Map.of("type", "CPF", "number", "12345678909"))))
                .statusCode(201)
                .body("method", is("CARD"))
                .body("status", is("APPROVED"))
                .body("expiresAt", nullValue())
                .body("pixQrCode", nullValue());
        getOrder(order).body("status", is("PENDING")).body("paid", is(true));

        // À vista, com o token e a bandeira do Brick; o e-mail do cartão vale sobre o do pedido.
        Map<String, Object> sent = MercadoPagoStub.requestOf(MercadoPagoStub.lastId());
        Map<?, ?> method =
                (Map<?, ?>)
                        ((Map<?, ?>)
                                        ((List<?>)
                                                        ((Map<?, ?>) sent.get("transactions"))
                                                                .get("payments"))
                                                .getFirst())
                                .get("payment_method");
        assertEquals("master", method.get("id"));
        assertEquals("credit_card", method.get("type"));
        assertEquals("tok-APRO", method.get("token"));
        assertEquals(1, method.get("installments"));
        Map<?, ?> payer = (Map<?, ?>) sent.get("payer");
        assertEquals("titular@example.com", payer.get("email"));
        assertEquals(Map.of("type", "CPF", "number", "12345678909"), payer.get("identification"));
    }

    @Test
    void declinedCardKeepsTheOrderWaitingAndCanBeRetried() {
        Placed order = placeGuest("CARD");

        pay(order, card("tok-OTHE"))
                .statusCode(201)
                .body("status", is("REJECTED"))
                .body("statusDetail", is("cc_rejected_insufficient_amount"));
        getOrder(order).body("status", is("AWAITING_PAYMENT")).body("paid", is(false));
        assertEquals(
                "ana@example.com",
                ((Map<?, ?>) MercadoPagoStub.requestOf(MercadoPagoStub.lastId()).get("payer"))
                        .get("email"));

        pay(order, card("tok-APRO")).statusCode(201).body("status", is("APPROVED"));
        given().header("X-Order-Token", order.token())
                .get("/orders/{id}/payments", order.id())
                .then()
                .body("", hasSize(2))
                .body("[0].status", is("REJECTED"))
                .body("[1].status", is("APPROVED"));
    }

    @Test
    void cardInReviewWaitsForTheWebhook() throws Exception {
        Placed order = placeGuest("CARD");

        pay(order, card("tok-CONT")).statusCode(201).body("status", is("PENDING"));
        String externalId = MercadoPagoStub.lastId();
        getOrder(order).body("status", is("AWAITING_PAYMENT"));
        // Em análise, a tentativa continua aberta.
        pay(order, card("tok-APRO")).statusCode(400);

        MercadoPagoStub.setStatus(externalId, "processed", "accredited");
        MercadoPagoWebhooks.send(externalId).statusCode(200);
        getOrder(order).body("status", is("PENDING")).body("paid", is(true));
    }

    @Test
    void cardFailureFromTheProviderIsRetriedWithTheSameKey() {
        Placed order = placeGuest("CARD");
        int before = MercadoPagoStub.created();

        MercadoPagoStub.failNext(1);
        pay(order, card("tok-APRO")).statusCode(502);
        pay(order, card("tok-APRO")).statusCode(201).body("status", is("APPROVED"));
        assertEquals(before + 1, MercadoPagoStub.created());
    }

    @Test
    void cardDataMustMatchTheMethod() {
        Placed cardOrder = placeGuest("CARD");
        Placed pixOrder = placeGuest("PIX");

        pay(cardOrder, Map.of("method", "CARD")).statusCode(400);
        pay(cardOrder, Map.of("method", "PIX")).statusCode(400);
        pay(pixOrder, card("tok-APRO")).statusCode(400);
        pay(
                        pixOrder,
                        Map.of(
                                "method",
                                "PIX",
                                "card",
                                Map.of("token", "tok-APRO", "paymentMethodId", "visa")))
                .statusCode(400);
        pay(cardOrder, Map.of("method", "CARD", "card", Map.of("paymentMethodId", "visa")))
                .statusCode(400);
        pay(
                        cardOrder,
                        Map.of(
                                "method",
                                "CARD",
                                "card",
                                Map.of(
                                        "token",
                                        "tok-APRO",
                                        "paymentMethodId",
                                        "visa",
                                        "identification",
                                        Map.of("type", "RG", "number", "123"))))
                .statusCode(400);
        // Domínio sem ponto: o Mercado Pago recusaria; a API responde 400 antes de cobrar.
        pay(
                        cardOrder,
                        Map.of(
                                "method",
                                "CARD",
                                "card",
                                Map.of(
                                        "token",
                                        "tok-APRO",
                                        "paymentMethodId",
                                        "visa",
                                        "payerEmail",
                                        "ana@gmailcom")))
                .statusCode(400);
        getOrder(cardOrder).body("status", is("AWAITING_PAYMENT"));
    }
}

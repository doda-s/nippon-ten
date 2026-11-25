package dev.nipponten;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.nipponten.application.services.PaymentService;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Reconciliação agendada, chamada direto com o relógio adiantado (o scheduler fica desligado). */
@QuarkusTest
class PaymentExpirationTest {

    @Inject PaymentService paymentService;

    @Inject TestClock clock;

    private record Placed(long id, String token) {}

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    private static Placed placeGuest(String method) {
        long size = TestFixtures.firstSizeOf(TestFixtures.createProduct("Pastel expira"));
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

    private static String lastPaymentStatus(Placed order) {
        List<String> statuses =
                given().header("X-Order-Token", order.token())
                        .get("/orders/{id}/payments", order.id())
                        .then()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getList("status");
        return statuses.getLast();
    }

    @Test
    void expiredPixCancelsTheOrder() {
        Placed order = placeGuest("PIX");
        pay(order, Map.of("method", "PIX")).statusCode(201);
        MercadoPagoStub.setStatus(MercadoPagoStub.lastId(), "canceled", "expired");

        clock.advance(Duration.ofMinutes(32));
        paymentService.reconcile();

        assertEquals("EXPIRED", lastPaymentStatus(order));
        getOrder(order)
                .body("status", is("CANCELLED"))
                .body("cancelReason", is("Payment not completed"))
                .body("paid", is(false));
    }

    @Test
    void approvalWithALostWebhookIsFoundByTheReconciliation() {
        Placed order = placeGuest("PIX");
        pay(order, Map.of("method", "PIX")).statusCode(201);
        MercadoPagoStub.setStatus(MercadoPagoStub.lastId(), "processed", "accredited");

        clock.advance(Duration.ofMinutes(32));
        paymentService.reconcile();

        assertEquals("APPROVED", lastPaymentStatus(order));
        getOrder(order).body("status", is("PENDING")).body("paid", is(true));
    }

    @Test
    void pixStillOpenAtTheProviderIsExpiredHere() {
        Placed order = placeGuest("PIX");
        pay(order, Map.of("method", "PIX")).statusCode(201);

        clock.advance(Duration.ofMinutes(32));
        paymentService.reconcile();

        assertEquals("EXPIRED", lastPaymentStatus(order));
        getOrder(order).body("status", is("CANCELLED"));
    }

    @Test
    void pixWithinTheDeadlineIsLeftAlone() {
        Placed order = placeGuest("PIX");
        pay(order, Map.of("method", "PIX")).statusCode(201);

        clock.advance(Duration.ofMinutes(20));
        paymentService.reconcile();

        assertEquals("PENDING", lastPaymentStatus(order));
        getOrder(order).body("status", is("AWAITING_PAYMENT"));
    }

    @Test
    void aCustomerCanReplaceAnExpiredPixBeforeTheJobRuns() {
        Placed order = placeGuest("PIX");
        pay(order, Map.of("method", "PIX")).statusCode(201);
        MercadoPagoStub.setStatus(MercadoPagoStub.lastId(), "canceled", "expired");

        // O Pix venceu e o job ainda não passou: a criação resolve o vencido na hora.
        clock.advance(Duration.ofMinutes(30).plusSeconds(10));
        pay(order, Map.of("method", "PIX")).statusCode(201).body("status", is("PENDING"));

        // Com um Pix novo aberto, o pedido espera ele em vez de ser cancelado.
        clock.advance(Duration.ofMinutes(1));
        paymentService.reconcile();
        getOrder(order).body("status", is("AWAITING_PAYMENT"));
    }

    @Test
    void orderWithoutAnyPaymentIsCancelledAfterTheTimeout() {
        Placed order = placeGuest("PIX");

        clock.advance(Duration.ofMinutes(10));
        paymentService.reconcile();
        getOrder(order).body("status", is("AWAITING_PAYMENT"));

        clock.advance(Duration.ofMinutes(21));
        paymentService.reconcile();
        getOrder(order).body("status", is("CANCELLED"));
        // Cancelado pelo sistema: o pagamento não é mais aceito.
        pay(order, Map.of("method", "PIX")).statusCode(400);
    }

    @Test
    void cardInReviewKeepsTheOrderWaiting() {
        Placed order = placeGuest("CARD");
        pay(
                        order,
                        Map.of(
                                "method",
                                "CARD",
                                "card",
                                Map.of("token", "tok-CONT", "paymentMethodId", "visa")))
                .statusCode(201);

        clock.advance(Duration.ofMinutes(45));
        paymentService.reconcile();

        assertEquals("PENDING", lastPaymentStatus(order));
        getOrder(order).body("status", is("AWAITING_PAYMENT"));
    }

    @Test
    void attemptThatNeverReachedTheProviderIsGivenUp() {
        Placed order = placeGuest("CARD");
        MercadoPagoStub.failNext(1);
        pay(
                        order,
                        Map.of(
                                "method",
                                "CARD",
                                "card",
                                Map.of("token", "tok-APRO", "paymentMethodId", "visa")))
                .statusCode(502);

        clock.advance(Duration.ofMinutes(31));
        paymentService.reconcile();

        assertEquals("EXPIRED", lastPaymentStatus(order));
        getOrder(order).body("status", is("CANCELLED"));
    }
}

package dev.nipponten;

import static dev.nipponten.TestAuth.asAdmin;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

@QuarkusTest
class PaymentRefundTest {

    @Inject PaymentService paymentService;

    @Inject TestClock clock;

    @AfterEach
    void resetClockAndStub() {
        clock.reset();
        MercadoPagoStub.rejectRefunds(0);
    }

    private record Placed(long id, String token) {}

    private static Placed placeGuest(String method) {
        long size = TestFixtures.firstSizeOf(TestFixtures.createProduct("Pastel estorno"));
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

    private static Map<String, Object> card(String token) {
        return Map.of("method", "CARD", "card", Map.of("token", token, "paymentMethodId", "visa"));
    }

    private static ValidatableResponse cancelAsCustomer(Placed order) {
        return given().header("X-Order-Token", order.token())
                .contentType(ContentType.JSON)
                .post("/orders/{id}/cancel", order.id())
                .then();
    }

    private static ValidatableResponse cancelAsStaff(Placed order) {
        return asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("reason", "Acabou o recheio"))
                .post("/orders/{id}/cancel", order.id())
                .then();
    }

    private static String lastPaymentStatus(Placed order) {
        return lastPayment(order, "status");
    }

    private static String lastPayment(Placed order, String field) {
        List<String> values =
                given().header("X-Order-Token", order.token())
                        .get("/orders/{id}/payments", order.id())
                        .then()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getList(field);
        return values.getLast();
    }

    // Pix aprovado pelo webhook: o pedido sai de AWAITING_PAYMENT para PENDING.
    private static String paidPix(Placed order) throws Exception {
        pay(order, Map.of("method", "PIX")).statusCode(201);
        String externalId = MercadoPagoStub.lastId();
        MercadoPagoStub.setStatus(externalId, "processed", "accredited");
        MercadoPagoWebhooks.send(externalId).statusCode(200);
        return externalId;
    }

    @Test
    void cancellingWithAnOpenPixCancelsTheCharge() {
        Placed order = placeGuest("PIX");
        pay(order, Map.of("method", "PIX")).statusCode(201);
        String externalId = MercadoPagoStub.lastId();

        cancelAsCustomer(order).statusCode(200).body("status", is("CANCELLED"));

        assertEquals("CANCELLED", lastPaymentStatus(order));
        assertEquals(1, MercadoPagoStub.operationKeysOf(externalId).size());
    }

    @Test
    void customerCancellingAPaidOrderGetsTheMoneyBack() throws Exception {
        Placed order = placeGuest("PIX");
        String externalId = paidPix(order);

        cancelAsCustomer(order).statusCode(200).body("status", is("CANCELLED"));

        assertEquals("REFUNDED", lastPaymentStatus(order));
        // A chave do estorno deriva da tentativa: repetir não estorna duas vezes.
        assertEquals(1, MercadoPagoStub.operationKeysOf(externalId).size());
        assertTrue(MercadoPagoStub.operationKeysOf(externalId).getFirst().endsWith("-refund"));
    }

    @Test
    void staffCancellingAPaidCardOrderInTheKitchenRefundsIt() {
        Placed order = placeGuest("CARD");
        pay(order, card("tok-APRO")).statusCode(201).body("status", is("APPROVED"));
        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("status", "IN_PREPARATION"))
                .post("/orders/{id}/status", order.id())
                .then()
                .statusCode(200);

        // O cliente não cancela depois de a cozinha começar; o funcionário cancela e estorna.
        cancelAsCustomer(order).statusCode(400);
        cancelAsStaff(order).statusCode(200).body("status", is("CANCELLED"));
        assertEquals("REFUNDED", lastPaymentStatus(order));
    }

    @Test
    void aFailedRefundStillCancelsAndTheReconciliationRefundsLater() throws Exception {
        Placed order = placeGuest("PIX");
        paidPix(order);

        // O Mercado Pago recusa por alguns segundos o estorno de um cartão recém-aprovado.
        MercadoPagoStub.failNext(1);
        cancelAsStaff(order).statusCode(200).body("status", is("CANCELLED")).body("paid", is(true));
        assertEquals("APPROVED", lastPaymentStatus(order));

        paymentService.reconcile();
        assertEquals("REFUNDED", lastPaymentStatus(order));
    }

    @Test
    void aFailedCancelOfAnOpenPixStillCancelsTheOrder() throws Exception {
        Placed order = placeGuest("PIX");
        pay(order, Map.of("method", "PIX")).statusCode(201);
        String externalId = MercadoPagoStub.lastId();

        MercadoPagoStub.failNext(1);
        cancelAsCustomer(order).statusCode(200).body("status", is("CANCELLED"));
        assertEquals("PENDING", lastPaymentStatus(order));

        // Pago antes de vencer: a aprovação de pedido cancelado é estornada.
        MercadoPagoStub.setStatus(externalId, "processed", "accredited");
        MercadoPagoWebhooks.send(externalId).statusCode(200);
        assertEquals("REFUNDED", lastPaymentStatus(order));
    }

    @Test
    void approvalAfterTheCancellationIsRefunded() throws Exception {
        Placed order = placeGuest("CARD");
        pay(order, card("tok-CONT")).statusCode(201).body("status", is("PENDING"));
        String externalId = MercadoPagoStub.lastId();

        // Cartão em análise não pode ser cancelado no provedor; o pedido é cancelado mesmo assim.
        cancelAsCustomer(order).statusCode(200);
        assertEquals("PENDING", lastPaymentStatus(order));

        MercadoPagoStub.setStatus(externalId, "processed", "accredited");
        MercadoPagoWebhooks.send(externalId).statusCode(200);

        assertEquals("REFUNDED", lastPaymentStatus(order));
        given().header("X-Order-Token", order.token())
                .get("/orders/{id}", order.id())
                .then()
                .body("status", is("CANCELLED"))
                .body("paid", is(false));
    }

    @Test
    void theReconciliationRetriesARefundThatFailed() throws Exception {
        Placed order = placeGuest("CARD");
        pay(order, card("tok-CONT")).statusCode(201);
        String externalId = MercadoPagoStub.lastId();
        cancelAsCustomer(order).statusCode(200);

        // A aprovação chega com o provedor instável: o estorno falha e fica para o job.
        MercadoPagoStub.setStatus(externalId, "processed", "accredited");
        MercadoPagoStub.failNext(1);
        MercadoPagoWebhooks.send(externalId).statusCode(200);
        assertEquals("APPROVED", lastPaymentStatus(order));

        paymentService.reconcile();
        assertEquals("REFUNDED", lastPaymentStatus(order));
    }

    @Test
    void aRefundRejectedForAFewMinutesGoesThroughOnTheNextRound() throws Exception {
        Placed order = placeGuest("CARD");
        pay(order, card("tok-APRO")).statusCode(201);

        // Cartão recém-aprovado: o Mercado Pago recusa o estorno (422) e aceita minutos depois.
        MercadoPagoStub.rejectRefunds(1);
        cancelAsCustomer(order).statusCode(200).body("status", is("CANCELLED"));
        assertEquals("APPROVED", lastPaymentStatus(order));

        clock.advance(Duration.ofMinutes(5));
        paymentService.reconcile();
        assertEquals("REFUNDED", lastPaymentStatus(order));
    }

    @Test
    void aRefundStillRejectedAfterAnHourIsLeftForManualRefund() throws Exception {
        Placed order = placeGuest("PIX");
        String externalId = paidPix(order);

        // Order paga sem o dinheiro ter entrado (visto no sandbox): o estorno é recusado sempre.
        MercadoPagoStub.rejectRefunds(Integer.MAX_VALUE);
        cancelAsCustomer(order).statusCode(200).body("status", is("CANCELLED"));

        clock.advance(Duration.ofMinutes(30));
        paymentService.reconcile();
        assertEquals("APPROVED", lastPaymentStatus(order));
        assertEquals("accredited", lastPayment(order, "statusDetail"));

        clock.advance(Duration.ofMinutes(31));
        paymentService.reconcile();
        assertEquals("APPROVED", lastPaymentStatus(order));
        assertEquals("refund_rejected", lastPayment(order, "statusDetail"));

        // Desistiu: as próximas rodadas não chamam o provedor de novo.
        int attempts = MercadoPagoStub.operationKeysOf(externalId).size();
        paymentService.reconcile();
        assertEquals(attempts, MercadoPagoStub.operationKeysOf(externalId).size());
    }

    @Test
    void chargebackIsRecordedWithoutChangingTheOrder() throws Exception {
        Placed order = placeGuest("CARD");
        pay(order, card("tok-APRO")).statusCode(201);
        String externalId = MercadoPagoStub.lastId();

        MercadoPagoStub.setStatus(externalId, "charged_back", "charged_back");
        MercadoPagoWebhooks.send(externalId).statusCode(200);

        assertEquals("CHARGED_BACK", lastPaymentStatus(order));
        given().header("X-Order-Token", order.token())
                .get("/orders/{id}", order.id())
                .then()
                .body("status", is("PENDING"))
                .body("paid", is(true));
    }

    @Test
    void refundMadeInTheProviderPanelIsRecorded() throws Exception {
        Placed order = placeGuest("PIX");
        String externalId = paidPix(order);

        MercadoPagoStub.setStatus(externalId, "processed", "refunded");
        MercadoPagoWebhooks.send(externalId).statusCode(200);

        assertEquals("REFUNDED", lastPaymentStatus(order));
    }
}

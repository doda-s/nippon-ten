package dev.nipponten;

import static dev.nipponten.TestAuth.as;
import static dev.nipponten.TestAuth.asAdmin;
import static dev.nipponten.TestAuth.login;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.nipponten.application.services.TrackingLinkLimiter;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OrderTrackingTest {

    private static final String LINK_BASE = "http://localhost:5173/pedido/";

    @Inject MockMailbox mailbox;

    @Inject TrackingLinkLimiter limiter;

    private record Placed(long id, String code, String token, String email) {}

    @BeforeEach
    void resetLimiter() {
        limiter.reset();
    }

    private static String uniqueEmail(String name) {
        return name + "." + System.nanoTime() + "@example.com";
    }

    private static Map<String, Object> guestBody(String name, String email, String channel) {
        long size = TestFixtures.firstSizeOf(TestFixtures.createProduct("Pastel rastreio"));
        Map<String, Object> body = new HashMap<>();
        body.put("items", List.of(Map.of("type", "PRODUCT", "productSizeId", size, "quantity", 1)));
        body.put("fulfillmentType", "PICKUP");
        body.put("paymentMethod", "PIX");
        body.put("paymentChannel", channel);
        body.put("guestName", name);
        body.put("guestPhone", "11955554444");
        if (email != null) body.put("guestEmail", email);
        return body;
    }

    private static Placed placeGuest(String name, String channel) {
        String email = uniqueEmail("visitante");
        JsonPath created =
                given().contentType(ContentType.JSON)
                        .body(guestBody(name, email, channel))
                        .post("/orders")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath();
        return new Placed(
                created.getLong("order.id"),
                created.getString("order.code"),
                created.getString("trackingToken"),
                email);
    }

    // O envio é assíncrono (o mailer reativo não segura a request), então a mensagem pode chegar
    // à caixa um pouco depois da resposta.
    private List<Mail> awaitMails(String to, int count) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            List<Mail> mails = mailbox.getMailsSentTo(to);
            if (mails != null && mails.size() >= count) return mails;
            Thread.sleep(100);
        }
        throw new AssertionError("Expected " + count + " e-mail(s) to " + to);
    }

    private static void resend(String code, String email) {
        given().contentType(ContentType.JSON)
                .body(Map.of("code", code, "email", email))
                .post("/orders/tracking-link")
                .then()
                .statusCode(202);
    }

    private static String tokenOf(Mail mail) {
        String text = mail.getText();
        int start = text.indexOf("#t=") + 3;
        int end = text.indexOf('\n', start);
        return text.substring(start, end < 0 ? text.length() : end).trim();
    }

    @Test
    void guestReceivesTheCodeAndTheTrackingLink() throws Exception {
        Placed order = placeGuest("Ana", "IN_PERSON");

        Mail mail = awaitMails(order.email(), 1).getFirst();
        String link = LINK_BASE + order.code() + "#t=" + order.token();
        assertEquals("Pedido " + order.code() + " recebido", mail.getSubject());
        assertTrue(mail.getText().contains(link));
        assertTrue(mail.getHtml().contains("href=\"" + link + "\""));
    }

    @Test
    void onlineOrderEmailSaysItWaitsForThePayment() throws Exception {
        Placed order = placeGuest("Ana", "ONLINE");

        Mail mail = awaitMails(order.email(), 1).getFirst();
        assertTrue(mail.getText().contains("Assim que o pagamento for confirmado"));
    }

    @Test
    void guestNameIsEscapedInTheHtml() throws Exception {
        Placed order = placeGuest("<b>Ana</b>", "IN_PERSON");

        Mail mail = awaitMails(order.email(), 1).getFirst();
        assertTrue(mail.getHtml().contains("&lt;b&gt;Ana&lt;/b&gt;"));
        assertFalse(mail.getHtml().contains("<b>Ana</b>"));
    }

    @Test
    void guestMustInformTheEmailAndAClientMustNot() {
        given().contentType(ContentType.JSON)
                .body(guestBody("Ana", null, "IN_PERSON"))
                .post("/orders")
                .then()
                .statusCode(400);
        given().contentType(ContentType.JSON)
                .body(guestBody("Ana", "nao-e-email", "IN_PERSON"))
                .post("/orders")
                .then()
                .statusCode(400);
        // Domínio sem ponto: passa no @Email puro, mas o Mercado Pago recusa o pagador.
        given().contentType(ContentType.JSON)
                .body(guestBody("Ana", "ana@gmailcom", "ONLINE"))
                .post("/orders")
                .then()
                .statusCode(400)
                .body("violations[0].field", containsString("guestEmail"));

        String email = uniqueEmail("cliente");
        String token = registerClient(email);
        Map<String, Object> body = guestBody("Ana", "outro@example.com", "IN_PERSON");
        body.remove("guestName");
        body.remove("guestPhone");
        as(token).contentType(ContentType.JSON).body(body).post("/orders").then().statusCode(400);
    }

    @Test
    void clientReceivesTheLinkWithoutTheToken() throws Exception {
        String email = uniqueEmail("cliente");
        String token = registerClient(email);
        Map<String, Object> body = guestBody("Ana", null, "IN_PERSON");
        body.remove("guestName");
        body.remove("guestPhone");
        String code =
                as(token)
                        .contentType(ContentType.JSON)
                        .body(body)
                        .post("/orders")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getString("order.code");

        Mail mail = awaitMails(email, 1).getFirst();
        assertTrue(mail.getText().contains("Olá, Joao!"));
        assertTrue(mail.getText().contains(LINK_BASE + code));
        assertFalse(mail.getText().contains("#t="));

        // O cliente abre o link logado, sem token.
        as(token).get("/orders/by-code/{code}", code).then().statusCode(200);
    }

    @Test
    void staffGuestOrderWithoutEmailIsAccepted() {
        Map<String, Object> body = guestBody("Mesa 2", null, "IN_PERSON");
        body.remove("guestPhone");
        asAdmin()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/orders")
                .then()
                .statusCode(201)
                .body("order.guestEmail", is((Object) null));
    }

    @Test
    void theLinkOpensTheOrderByCode() {
        Placed order = placeGuest("Ana", "IN_PERSON");
        Placed other = placeGuest("Bia", "IN_PERSON");

        given().header("X-Order-Token", order.token())
                .get("/orders/by-code/{code}", order.code())
                .then()
                .statusCode(200)
                .body("id", is((int) order.id()))
                .body("guestEmail", is(order.email()));
        given().header("X-Order-Token", order.token())
                .get("/orders/by-code/{code}", order.code().toLowerCase())
                .then()
                .statusCode(200);

        given().get("/orders/by-code/{code}", order.code()).then().statusCode(401);
        given().header("X-Order-Token", other.token())
                .get("/orders/by-code/{code}", order.code())
                .then()
                .statusCode(403);
        // Código inexistente é 403, como o de outro pedido: não revela quais existem.
        given().header("X-Order-Token", order.token())
                .get("/orders/by-code/{code}", "ZZZZZZ")
                .then()
                .statusCode(403);

        asAdmin().get("/orders/by-code/{code}", order.code()).then().statusCode(200);
        asAdmin().get("/orders/by-code/{code}", "ZZZZZZ").then().statusCode(404);
    }

    @Test
    void resendingTheLinkReplacesTheToken() throws Exception {
        Placed order = placeGuest("Ana", "IN_PERSON");
        awaitMails(order.email(), 1);

        resend(order.code().toLowerCase(), order.email().toUpperCase());

        Mail resent = awaitMails(order.email(), 2).get(1);
        assertEquals("Link para acompanhar o pedido " + order.code(), resent.getSubject());
        String newToken = tokenOf(resent);
        given().header("X-Order-Token", newToken)
                .get("/orders/{id}", order.id())
                .then()
                .statusCode(200);
        given().header("X-Order-Token", order.token())
                .get("/orders/{id}", order.id())
                .then()
                .statusCode(403);
    }

    @Test
    void wrongEmailOrUnknownCodeSendsNothing() throws Exception {
        Placed order = placeGuest("Ana", "IN_PERSON");
        awaitMails(order.email(), 1);

        resend(order.code(), "outra.pessoa@example.com");
        resend("ZZZZZZ", order.email());

        Thread.sleep(300);
        assertEquals(1, mailbox.getMailsSentTo(order.email()).size());
        assertTrue(
                mailbox.getMailsSentTo("outra.pessoa@example.com") == null
                        || mailbox.getMailsSentTo("outra.pessoa@example.com").isEmpty());
        // O token original continua valendo.
        given().header("X-Order-Token", order.token())
                .get("/orders/{id}", order.id())
                .then()
                .statusCode(200);
    }

    @Test
    void resendingIsRateLimitedPerOrder() {
        Placed order = placeGuest("Ana", "IN_PERSON");

        for (int i = 0; i < 3; i++) {
            resend(order.code(), "errado@example.com");
        }
        given().contentType(ContentType.JSON)
                .body(Map.of("code", order.code(), "email", order.email()))
                .post("/orders/tracking-link")
                .then()
                .statusCode(429);
    }

    @Test
    void resendRequiresCodeAndEmail() {
        given().contentType(ContentType.JSON)
                .body(Map.of("code", "ABC123"))
                .post("/orders/tracking-link")
                .then()
                .statusCode(400);
        given().contentType(ContentType.JSON)
                .body(Map.of("code", "ABC123", "email", "nao-e-email"))
                .post("/orders/tracking-link")
                .then()
                .statusCode(400);
    }

    private static String registerClient(String email) {
        given().contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "user", Map.of("email", email, "password", "senha-forte-123"),
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
                .statusCode(201);
        return login(email, "senha-forte-123");
    }
}

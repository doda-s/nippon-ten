package dev.nipponten;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import dev.nipponten.application.services.LoginAttemptLimiter;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class LoginRateLimitTest {

    private static final String PASSWORD = "secret123";
    private static final String TOO_MANY = "Too many login attempts, try again later";

    @Inject LoginAttemptLimiter limiter;

    // Todos os requests dos testes saem de 127.0.0.1, então o contador por IP é compartilhado.
    @BeforeEach
    @AfterEach
    void resetLimiter() {
        limiter.reset();
    }

    private static String uniqueEmail() {
        return "limite." + System.nanoTime() + "@example.com";
    }

    private static void registerClient(String email) {
        given().contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "user", Map.of("email", email, "password", PASSWORD),
                                "client", Map.of("name", "Joao", "lastName", "Sumi")))
                .post("/users")
                .then()
                .statusCode(201);
    }

    private static ValidatableResponse login(String email, String password) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .post("/auth/login")
                .then();
    }

    private static void failTimes(String email, int times) {
        for (int i = 0; i < times; i++) {
            login(email, "wrong-password").statusCode(401);
        }
    }

    @Test
    void fiveFailuresBlockTheAccountEvenWithTheRightPassword() {
        String email = uniqueEmail();
        registerClient(email);
        failTimes(email, 5);

        login(email, PASSWORD)
                .statusCode(429)
                .contentType(ContentType.JSON)
                .header("Retry-After", notNullValue())
                .body("message", is(TOO_MANY));
        // O email é normalizado: trocar a caixa não contorna o limite.
        login(email.toUpperCase(), PASSWORD).statusCode(429);

        String other = uniqueEmail();
        registerClient(other);
        login(other, PASSWORD).statusCode(200);
    }

    @Test
    void unknownEmailsAreLimitedTheSameWay() {
        String email = uniqueEmail();
        failTimes(email, 5);

        login(email, PASSWORD).statusCode(429);
    }

    @Test
    void successfulLoginResetsTheAccountCounter() {
        String email = uniqueEmail();
        registerClient(email);
        failTimes(email, 4);
        login(email, PASSWORD).statusCode(200);
        failTimes(email, 4);

        login(email, PASSWORD).statusCode(200);
    }

    @Test
    void twentyFailuresFromOneIpBlockEveryAccountFromIt() {
        for (int i = 0; i < 20; i++) {
            login(uniqueEmail(), "wrong-password").statusCode(401);
        }
        String email = uniqueEmail();
        registerClient(email);

        login(email, PASSWORD).statusCode(429).body("message", is(TOO_MANY));
    }
}

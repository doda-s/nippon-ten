package dev.nipponten;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Jobs disparados por HTTP pelo Cloud Scheduler (POST /jobs/...), autenticados por token. */
@QuarkusTest
class JobTriggerTest {

    private static final String TOKEN = "test-jobs-token";

    @Inject TestClock clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void reconciliationWithoutTokenIsRejected() {
        given().post("/jobs/payment-reconciliation")
                .then()
                .statusCode(401)
                .body("message", is("Invalid job token"));
    }

    @Test
    void reconciliationWithAWrongTokenIsRejected() {
        given().header("X-Job-Token", "wrong-token")
                .post("/jobs/payment-reconciliation")
                .then()
                .statusCode(401)
                .body("message", is("Invalid job token"));
    }

    @Test
    void reconciliationWithTheTokenCancelsAnAbandonedOrder() {
        long size = TestFixtures.firstSizeOf(TestFixtures.createProduct("Pastel do job"));
        Map<String, Object> body = new HashMap<>();
        body.put("items", List.of(Map.of("type", "PRODUCT", "productSizeId", size, "quantity", 1)));
        body.put("fulfillmentType", "PICKUP");
        body.put("paymentMethod", "PIX");
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
        long orderId = created.getLong("order.id");
        String trackingToken = created.getString("trackingToken");

        clock.advance(Duration.ofMinutes(31));
        given().header("X-Job-Token", TOKEN)
                .post("/jobs/payment-reconciliation")
                .then()
                .statusCode(204);

        given().header("X-Order-Token", trackingToken)
                .get("/orders/{id}", orderId)
                .then()
                .statusCode(200)
                .body("status", is("CANCELLED"));
    }
}

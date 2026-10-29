package dev.nipponten;

import static dev.nipponten.TestAuth.as;
import static dev.nipponten.TestAuth.asAdmin;
import static dev.nipponten.TestAuth.login;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OrderFeedbackTest {

    private record Placed(long id, String token) {}

    private static Map<String, Object> pickupOrder() {
        long size = TestFixtures.firstSizeOf(TestFixtures.createProduct("Pastel avaliado"));
        Map<String, Object> body = new HashMap<>();
        body.put("items", List.of(Map.of("type", "PRODUCT", "productSizeId", size, "quantity", 1)));
        body.put("fulfillmentType", "PICKUP");
        body.put("paymentMethod", "CARD");
        return body;
    }

    private static Placed place(RequestSpecification spec, Map<String, Object> body) {
        JsonPath created =
                spec.contentType(ContentType.JSON)
                        .body(body)
                        .post("/orders")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath();
        return new Placed(created.getLong("order.id"), created.getString("trackingToken"));
    }

    private static Placed placeGuestOrder() {
        Map<String, Object> body = pickupOrder();
        body.put("guestName", "Caio");
        body.put("guestPhone", "11966665555");
        return place(given(), body);
    }

    private static void complete(long id) {
        for (String status : List.of("IN_PREPARATION", "READY", "COMPLETED")) {
            asAdmin()
                    .contentType(ContentType.JSON)
                    .body(Map.of("status", status))
                    .post("/orders/{id}/status", id)
                    .then()
                    .statusCode(200);
        }
    }

    private static ValidatableResponse feedback(
            RequestSpecification spec, long id, Map<String, Object> body) {
        return spec.contentType(ContentType.JSON)
                .body(body)
                .post("/orders/{id}/feedback", id)
                .then();
    }

    @Test
    void guestRatesACompletedOrderOnce() {
        Placed order = placeGuestOrder();
        complete(order.id());
        RequestSpecification guest = given().header("X-Order-Token", order.token());

        feedback(guest, order.id(), Map.of("rating", 5, "comment", "Muito bom"))
                .statusCode(201)
                .body("feedback.rating", is(5))
                .body("feedback.comment", is("Muito bom"))
                .body("feedback.createdAt", notNullValue());
        given().header("X-Order-Token", order.token())
                .get("/orders/{id}", order.id())
                .then()
                .statusCode(200)
                .body("feedback.rating", is(5));
        feedback(given().header("X-Order-Token", order.token()), order.id(), Map.of("rating", 4))
                .statusCode(400);
    }

    @Test
    void feedbackOnlyAfterTheOrderIsCompleted() {
        Placed order = placeGuestOrder();

        given().header("X-Order-Token", order.token())
                .get("/orders/{id}", order.id())
                .then()
                .body("feedback", nullValue());
        feedback(given().header("X-Order-Token", order.token()), order.id(), Map.of("rating", 3))
                .statusCode(400);
    }

    @Test
    void ratingMustBeBetweenOneAndFive() {
        Placed order = placeGuestOrder();
        complete(order.id());

        feedback(given().header("X-Order-Token", order.token()), order.id(), Map.of("rating", 6))
                .statusCode(400);
        feedback(given().header("X-Order-Token", order.token()), order.id(), Map.of("rating", 0))
                .statusCode(400);
    }

    @Test
    void clientRatesTheirOwnOrder() {
        String email = "avaliador." + System.nanoTime() + "@example.com";
        given().contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "user", Map.of("email", email, "password", "senha-forte-123"),
                                "client", Map.of("name", "Lia", "lastName", "Souza")))
                .post("/users")
                .then()
                .statusCode(201);
        String token = login(email, "senha-forte-123");
        Placed order = place(as(token), pickupOrder());
        complete(order.id());

        feedback(as(token), order.id(), Map.of("rating", 4)).statusCode(201);
    }

    @Test
    void staffCannotRateOnBehalfOfTheCustomer() {
        Placed order = placeGuestOrder();
        complete(order.id());

        feedback(asAdmin(), order.id(), Map.of("rating", 5)).statusCode(403);
    }

    @Test
    void strangersCannotRate() {
        Placed order = placeGuestOrder();
        complete(order.id());

        feedback(given(), order.id(), Map.of("rating", 5)).statusCode(401);
        feedback(given().header("X-Order-Token", "token-errado"), order.id(), Map.of("rating", 5))
                .statusCode(403);
    }
}

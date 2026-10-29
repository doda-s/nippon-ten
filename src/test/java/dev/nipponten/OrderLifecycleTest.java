package dev.nipponten;

import static dev.nipponten.TestAuth.as;
import static dev.nipponten.TestAuth.asAdmin;
import static dev.nipponten.TestAuth.login;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OrderLifecycleTest {

    private record Placed(long id, String token, long productSizeId) {}

    private static Map<String, Object> item(long productSizeId, int quantity) {
        return Map.of("type", "PRODUCT", "productSizeId", productSizeId, "quantity", quantity);
    }

    private static Placed placeGuestOrder(String fulfillment) {
        long size = TestFixtures.firstSizeOf(TestFixtures.createProduct("Pastel do ciclo"));
        Map<String, Object> body = new HashMap<>();
        body.put("items", List.of(item(size, 1)));
        body.put("fulfillmentType", fulfillment);
        body.put("paymentMethod", "PIX");
        body.put("guestName", "Ana");
        body.put("guestPhone", "11988887777");
        if (fulfillment.equals("DELIVERY")) {
            body.put(
                    "address", Map.of("streetAddress", "Rua B", "number", "2", "cep", "02000-000"));
        }
        JsonPath created =
                given().contentType(ContentType.JSON)
                        .body(body)
                        .post("/orders")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath();
        return new Placed(created.getLong("order.id"), created.getString("trackingToken"), size);
    }

    private static ValidatableResponse moveTo(long id, String status) {
        return asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("status", status))
                .post("/orders/{id}/status", id)
                .then();
    }

    private static ValidatableResponse cancelAsGuest(Placed order) {
        return given().header("X-Order-Token", order.token())
                .contentType(ContentType.JSON)
                .post("/orders/{id}/cancel", order.id())
                .then();
    }

    @Test
    void staffMovesAPickupOrderStepByStep() {
        Placed order = placeGuestOrder("PICKUP");

        moveTo(order.id(), "IN_PREPARATION").statusCode(200).body("status", is("IN_PREPARATION"));
        moveTo(order.id(), "READY").statusCode(200);
        // Retirada não sai para entrega.
        moveTo(order.id(), "OUT_FOR_DELIVERY").statusCode(400);
        moveTo(order.id(), "COMPLETED")
                .statusCode(200)
                .body("status", is("COMPLETED"))
                .body("statusHistory", hasSize(4))
                .body("statusHistory[3].status", is("COMPLETED"));
        moveTo(order.id(), "COMPLETED").statusCode(400);
    }

    @Test
    void deliveryGoesOutForDeliveryBeforeCompleting() {
        Placed order = placeGuestOrder("DELIVERY");
        moveTo(order.id(), "IN_PREPARATION").statusCode(200);
        moveTo(order.id(), "READY").statusCode(200);

        moveTo(order.id(), "COMPLETED").statusCode(400);
        moveTo(order.id(), "OUT_FOR_DELIVERY").statusCode(200);
        moveTo(order.id(), "COMPLETED").statusCode(200);
    }

    @Test
    void rejectsSkippingAStatus() {
        Placed order = placeGuestOrder("PICKUP");

        moveTo(order.id(), "READY").statusCode(400);
        moveTo(order.id(), "CANCELLED").statusCode(400);
    }

    @Test
    void guestCancelsWhilePending() {
        Placed order = placeGuestOrder("PICKUP");

        cancelAsGuest(order)
                .statusCode(200)
                .body("status", is("CANCELLED"))
                .body("statusHistory[1].status", is("CANCELLED"));
        moveTo(order.id(), "IN_PREPARATION").statusCode(400);
    }

    @Test
    void guestCannotCancelOnceTheKitchenStarted() {
        Placed order = placeGuestOrder("PICKUP");
        moveTo(order.id(), "IN_PREPARATION").statusCode(200);

        cancelAsGuest(order).statusCode(400);
    }

    @Test
    void staffCancelsLaterButMustGiveAReason() {
        Placed order = placeGuestOrder("PICKUP");
        moveTo(order.id(), "IN_PREPARATION").statusCode(200);

        asAdmin()
                .contentType(ContentType.JSON)
                .post("/orders/{id}/cancel", order.id())
                .then()
                .statusCode(400);
        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("reason", "Sem gas"))
                .post("/orders/{id}/cancel", order.id())
                .then()
                .statusCode(200)
                .body("status", is("CANCELLED"))
                .body("cancelReason", is("Sem gas"));
    }

    @Test
    void strangersCannotCancel() {
        Placed order = placeGuestOrder("PICKUP");
        String email = "estranho." + System.nanoTime() + "@example.com";
        given().contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "user", Map.of("email", email, "password", "senha-forte-123"),
                                "client", Map.of("name", "Outro", "lastName", "Cliente")))
                .post("/users")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .post("/orders/{id}/cancel", order.id())
                .then()
                .statusCode(401);
        as(login(email, "senha-forte-123"))
                .contentType(ContentType.JSON)
                .post("/orders/{id}/cancel", order.id())
                .then()
                .statusCode(403);
    }

    @Test
    void marksAnOrderAsPaidOnlyOnce() {
        Placed order = placeGuestOrder("PICKUP");

        asAdmin()
                .post("/orders/{id}/pay", order.id())
                .then()
                .statusCode(200)
                .body("paid", is(true))
                .body("paidAt", notNullValue())
                .body("status", is("PENDING"));
        asAdmin().post("/orders/{id}/pay", order.id()).then().statusCode(400);
    }

    @Test
    void staffEditsTheItemsAndTheOrderIsRepriced() {
        Placed order = placeGuestOrder("PICKUP");

        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("items", List.of(item(order.productSizeId(), 3))))
                .put("/orders/{id}/items", order.id())
                .then()
                .statusCode(200)
                .body("items", hasSize(1))
                .body("items[0].quantity", is(3))
                .body("total", is(30.0f));

        moveTo(order.id(), "IN_PREPARATION").statusCode(200);
        moveTo(order.id(), "READY").statusCode(200);
        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("items", List.of(item(order.productSizeId(), 1))))
                .put("/orders/{id}/items", order.id())
                .then()
                .statusCode(400);
    }

    @Test
    void listsOrdersByStatusForTheDashboard() {
        Placed cancelled = placeGuestOrder("PICKUP");
        cancelAsGuest(cancelled).statusCode(200);

        asAdmin()
                .queryParam("status", "CANCELLED")
                .queryParam("size", 100)
                .get("/orders")
                .then()
                .statusCode(200)
                .body("content.id", hasItem((int) cancelled.id()))
                .body("content.status", not(hasItem("PENDING")));
        asAdmin()
                .queryParam("status", "PENDING")
                .queryParam("size", 100)
                .get("/orders")
                .then()
                .statusCode(200)
                .body("content.id", not(hasItem((int) cancelled.id())));
    }
}

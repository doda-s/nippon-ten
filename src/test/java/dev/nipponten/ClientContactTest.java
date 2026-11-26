package dev.nipponten;

import static dev.nipponten.TestAuth.as;
import static dev.nipponten.TestAuth.asAdmin;
import static dev.nipponten.TestAuth.login;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// O telefone do cliente é obrigatório no cadastro, e a equipe vê nome e telefone no pedido.
@QuarkusTest
class ClientContactTest {

    private static final String PASSWORD = "senha-forte-123";

    private record Client(long userId, String token) {}

    private static Map<String, Object> registration(String email, Map<String, Object> client) {
        return Map.of("user", Map.of("email", email, "password", PASSWORD), "client", client);
    }

    private static Client newClient(String phone) {
        String email = "cliente.contato." + System.nanoTime() + "@example.com";
        long userId =
                given().contentType(ContentType.JSON)
                        .body(
                                registration(
                                        email,
                                        Map.of("name", "Ana", "lastName", "Lima", "phone", phone)))
                        .post("/users")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("id");
        return new Client(userId, login(email, PASSWORD));
    }

    private static Map<String, Object> pickup() {
        long product = TestFixtures.createProduct("Pastel do contato");
        Map<String, Object> body = new HashMap<>();
        body.put(
                "items",
                List.of(
                        Map.of(
                                "type",
                                "PRODUCT",
                                "productSizeId",
                                TestFixtures.firstSizeOf(product),
                                "quantity",
                                1)));
        body.put("fulfillmentType", "PICKUP");
        body.put("paymentMethod", "CASH");
        return body;
    }

    private static long placeAs(Client client) {
        return as(client.token())
                .contentType(ContentType.JSON)
                .body(pickup())
                .post("/orders")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("order.id");
    }

    @Test
    void registrationRequiresAPhone() {
        String email = "sem.telefone." + System.nanoTime() + "@example.com";

        given().contentType(ContentType.JSON)
                .body(registration(email, Map.of("name", "Ana", "lastName", "Lima")))
                .post("/users")
                .then()
                .statusCode(400)
                .body("violations.field", hasItem("phone"));
        given().contentType(ContentType.JSON)
                .body(
                        registration(
                                email,
                                Map.of("name", "Ana", "lastName", "Lima", "phone", "1".repeat(31))))
                .post("/users")
                .then()
                .statusCode(400);
    }

    @Test
    void theClientKeepsAndUpdatesThePhone() {
        Client client = newClient(" 11 98888-7777 ");

        as(client.token())
                .get("/users/{id}/client", client.userId())
                .then()
                .statusCode(200)
                .body("phone", is("11 98888-7777"));
        as(client.token())
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Ana", "lastName", "Lima"))
                .put("/users/{id}/client", client.userId())
                .then()
                .statusCode(400);
        as(client.token())
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Ana", "lastName", "Lima", "phone", "11977776666"))
                .put("/users/{id}/client", client.userId())
                .then()
                .statusCode(200)
                .body("phone", is("11977776666"));
    }

    @Test
    void theStaffSeesTheClientNameAndPhoneOnTheOrder() {
        Client client = newClient("11988887777");
        long id = placeAs(client);

        asAdmin()
                .get("/orders/{id}", id)
                .then()
                .statusCode(200)
                .body("clientName", is("Ana Lima"))
                .body("clientPhone", is("11988887777"))
                .body("guestName", nullValue());
        asAdmin()
                .queryParam("size", 100)
                .get("/orders")
                .then()
                .statusCode(200)
                .body("content.find { it.id == " + id + " }.clientName", is("Ana Lima"))
                .body("content.find { it.id == " + id + " }.clientPhone", is("11988887777"));
        as(client.token())
                .get("/users/{id}/orders", client.userId())
                .then()
                .statusCode(200)
                .body("content[0].clientName", is("Ana Lima"));
    }

    @Test
    void aGuestOrderHasNoClientContact() {
        Map<String, Object> body = pickup();
        body.put("guestName", "Maria");
        body.put("guestPhone", "11999990000");
        body.put("guestEmail", "maria@example.com");
        long id =
                given().contentType(ContentType.JSON)
                        .body(body)
                        .post("/orders")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("order.id");

        asAdmin()
                .get("/orders/{id}", id)
                .then()
                .statusCode(200)
                .body("guestPhone", is("11999990000"))
                .body("clientName", nullValue())
                .body("clientPhone", nullValue());
    }

    @Test
    void aDeletedAccountTakesTheContactOffItsOrders() {
        Client client = newClient("11988887777");
        long id = placeAs(client);

        as(client.token()).delete("/users/{id}", client.userId()).then().statusCode(204);

        asAdmin()
                .get("/orders/{id}", id)
                .then()
                .statusCode(200)
                .body("clientId", nullValue())
                .body("clientName", nullValue())
                .body("clientPhone", nullValue());
    }
}

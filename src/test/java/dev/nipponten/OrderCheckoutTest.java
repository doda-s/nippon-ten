package dev.nipponten;

import static dev.nipponten.TestAuth.as;
import static dev.nipponten.TestAuth.asAdmin;
import static dev.nipponten.TestAuth.login;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OrderCheckoutTest {

    private static final String PASSWORD = "senha-forte-123";

    private record Client(long userId, String token) {}

    private static Client newClient() {
        String email = "cliente.pedido." + System.nanoTime() + "@example.com";
        long userId =
                given().contentType(ContentType.JSON)
                        .body(
                                Map.of(
                                        "user", Map.of("email", email, "password", PASSWORD),
                                        "client", Map.of("name", "Joao", "lastName", "Sumi")))
                        .post("/users")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("id");
        return new Client(userId, login(email, PASSWORD));
    }

    private static long addAddress(Client client, String street) {
        return as(client.token())
                .contentType(ContentType.JSON)
                .body(Map.of("streetAddress", street, "number", "10", "cep", "01000-000"))
                .post("/users/{id}/addresses", client.userId())
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");
    }

    private static Map<String, Object> pastel() {
        long product = TestFixtures.createProduct("Pastel do pedido");
        return Map.of(
                "type",
                "PRODUCT",
                "productSizeId",
                TestFixtures.firstSizeOf(product),
                "quantity",
                1);
    }

    private static Map<String, Object> order(String fulfillment) {
        Map<String, Object> body = new HashMap<>();
        body.put("items", List.of(pastel()));
        body.put("fulfillmentType", fulfillment);
        body.put("paymentMethod", "CASH");
        return body;
    }

    private static Map<String, Object> guestOrder(String fulfillment) {
        Map<String, Object> body = order(fulfillment);
        body.put("guestName", "Maria");
        body.put("guestPhone", "11999990000");
        return body;
    }

    private static JsonPath place(RequestSpecification spec, Map<String, Object> body) {
        return spec.contentType(ContentType.JSON)
                .body(body)
                .post("/orders")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath();
    }

    @Test
    void guestPlacesAnOrderAndFollowsItWithTheTrackingToken() {
        JsonPath created = place(given(), guestOrder("PICKUP"));
        long id = created.getLong("order.id");
        String token = created.getString("trackingToken");

        Assertions.assertNotNull(token);
        Assertions.assertEquals(6, created.getString("order.code").length());
        Assertions.assertEquals("PENDING", created.getString("order.status"));
        Assertions.assertEquals(10.0f, created.getFloat("order.total"));

        given().header("X-Order-Token", token)
                .get("/orders/{id}", id)
                .then()
                .statusCode(200)
                .body("guestName", is("Maria"))
                .body("items", hasSize(1))
                .body("statusHistory[0].status", is("PENDING"));
        given().get("/orders/{id}", id).then().statusCode(401);
        given().header("X-Order-Token", "token-errado")
                .get("/orders/{id}", id)
                .then()
                .statusCode(403);
    }

    @Test
    void guestMustInformNameAndPhone() {
        given().contentType(ContentType.JSON)
                .body(order("PICKUP"))
                .post("/orders")
                .then()
                .statusCode(400);
    }

    @Test
    void deliveryRequiresAnAddressAndCopiesIt() {
        given().contentType(ContentType.JSON)
                .body(guestOrder("DELIVERY"))
                .post("/orders")
                .then()
                .statusCode(400);

        Map<String, Object> body = guestOrder("DELIVERY");
        body.put("address", Map.of("streetAddress", "Rua A", "number", "1", "cep", "01000-000"));
        JsonPath created = place(given(), body);
        Assertions.assertEquals("Rua A", created.getString("order.address.streetAddress"));
    }

    @Test
    void onlyDeliveryTakesAnAddress() {
        Map<String, Object> body = guestOrder("PICKUP");
        body.put("address", Map.of("streetAddress", "Rua A", "number", "1", "cep", "01000-000"));

        given().contentType(ContentType.JSON).body(body).post("/orders").then().statusCode(400);
    }

    @Test
    void clientOrdersWithASavedAddressAndSeesItInTheHistory() {
        Client client = newClient();
        long address = addAddress(client, "Rua do Cliente");
        Map<String, Object> body = order("DELIVERY");
        body.put("addressId", address);

        JsonPath created = place(as(client.token()), body);
        long id = created.getLong("order.id");
        Assertions.assertNull(created.get("trackingToken"));
        Assertions.assertEquals("Rua do Cliente", created.getString("order.address.streetAddress"));

        as(client.token()).get("/orders/{id}", id).then().statusCode(200);
        as(client.token())
                .get("/users/{id}/orders", client.userId())
                .then()
                .statusCode(200)
                .body("totalElements", is(1))
                .body("content[0].id", is((int) id))
                .body("content[0].itemCount", is(1));

        Client other = newClient();
        as(other.token()).get("/orders/{id}", id).then().statusCode(403);
        as(other.token()).get("/users/{id}/orders", client.userId()).then().statusCode(403);
    }

    @Test
    void clientCannotUseAnotherClientsAddress() {
        Client owner = newClient();
        long address = addAddress(owner, "Rua Alheia");
        Map<String, Object> body = order("DELIVERY");
        body.put("addressId", address);

        as(newClient().token())
                .contentType(ContentType.JSON)
                .body(body)
                .post("/orders")
                .then()
                .statusCode(404);
    }

    @Test
    void clientCannotOrderOnBehalfOfSomeoneElse() {
        Map<String, Object> body = order("PICKUP");
        body.put("guestName", "Outra pessoa");

        as(newClient().token())
                .contentType(ContentType.JSON)
                .body(body)
                .post("/orders")
                .then()
                .statusCode(400);
    }

    @Test
    void staffPlacesAnOrderForAClientAtTheCounter() {
        Client client = newClient();
        long clientId =
                as(client.token())
                        .get("/users/{id}/client", client.userId())
                        .then()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getLong("id");
        Map<String, Object> body = order("DINE_IN");
        body.put("clientId", clientId);

        JsonPath created = place(asAdmin(), body);
        long id = created.getLong("order.id");

        Assertions.assertEquals(clientId, created.getLong("order.clientId"));
        as(client.token()).get("/orders/{id}", id).then().statusCode(200);
        asAdmin()
                .get("/orders/{id}", id)
                .then()
                .statusCode(200)
                .body("clientId", is((int) clientId));
    }

    @Test
    void staffPlacesAGuestOrderWithJustAName() {
        Map<String, Object> body = order("DINE_IN");
        body.put("guestName", "Mesa 4");

        JsonPath created = place(asAdmin(), body);

        Assertions.assertNotNull(created.getString("trackingToken"));
        Assertions.assertNull(created.get("order.clientId"));
    }

    @Test
    void internalUserWithoutManageOrdersCannotPlaceOrders() {
        long roleId =
                asAdmin()
                        .contentType(ContentType.JSON)
                        .body(
                                Map.of(
                                        "name",
                                        "sem-pedidos-" + System.nanoTime(),
                                        "permissions",
                                        List.of()))
                        .post("/internal-roles")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("id");
        String email = "interno.pedido." + System.nanoTime() + "@example.com";
        asAdmin()
                .contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "user",
                                Map.of("email", email, "password", PASSWORD),
                                "internal",
                                Map.of(
                                        "internalRoleId",
                                        roleId,
                                        "userPermissions",
                                        List.of(),
                                        "name",
                                        "Interno",
                                        "lastName",
                                        "Teste",
                                        "cpf",
                                        "12345678901")))
                .post("/internal")
                .then()
                .statusCode(201);
        Map<String, Object> body = order("DINE_IN");
        body.put("guestName", "Mesa 1");

        as(login(email, PASSWORD))
                .contentType(ContentType.JSON)
                .body(body)
                .post("/orders")
                .then()
                .statusCode(403);
    }

    @Test
    void anInternalUserHasAnEmptyOrderHistory() {
        long adminId =
                asAdmin()
                        .get("/auth/me")
                        .then()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getLong("userId");

        asAdmin()
                .get("/users/{id}/orders", adminId)
                .then()
                .statusCode(200)
                .body("totalElements", is(0))
                .body("content", hasSize(0));
    }

    @Test
    void unknownOrderIsNotFoundForStaffAndForbiddenForOthers() {
        asAdmin().get("/orders/{id}", 999999).then().statusCode(404);
        as(newClient().token()).get("/orders/{id}", 999999).then().statusCode(403);
    }

    @Test
    void unavailableItemsPreventTheOrder() {
        Map<String, Object> body = guestOrder("PICKUP");
        body.put("items", List.of(Map.of("type", "COMBO", "productSizeId", 1, "quantity", 1)));

        given().contentType(ContentType.JSON)
                .body(body)
                .post("/orders")
                .then()
                .statusCode(400)
                .body("violations[0].field", is("items[0]"))
                .body("violations[0].message", notNullValue());
    }

    @Test
    void responseNeverExposesTheTokenHash() {
        JsonPath created = place(given(), guestOrder("PICKUP"));

        given().header("X-Order-Token", created.getString("trackingToken"))
                .get("/orders/{id}", created.getLong("order.id"))
                .then()
                .statusCode(200)
                .body("trackingTokenHash", nullValue());
    }
}

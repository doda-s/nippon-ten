package dev.nipponten;

import static dev.nipponten.TestAuth.as;
import static dev.nipponten.TestAuth.asAdmin;
import static dev.nipponten.TestAuth.login;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.nipponten.application.services.GeocodingService;
import dev.nipponten.domain.models.DeliverySettings;
import dev.nipponten.domain.repositories.DeliverySettingsRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DeliveryFeeTest {

    private static final String PASSWORD = "senha-forte-123";

    @Inject DeliverySettingsRepository settingsRepository;

    @Inject GeocodingService geocodingService;

    @Inject TestClock clock;

    // A configuração é compartilhada: os outros testes contam com o cálculo desligado.
    @AfterEach
    void turnTheCalculationOff() {
        settingsRepository.save(new DeliverySettings(null, List.of(), Instant.now(), null));
        clock.reset();
    }

    private static void configure() {
        asAdmin()
                .contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "storeLatitude", GeoStub.STORE_LATITUDE,
                                "storeLongitude", GeoStub.STORE_LONGITUDE,
                                "tiers",
                                        List.of(
                                                Map.of("maxDistanceKm", 6, "fee", 8),
                                                Map.of("maxDistanceKm", 3, "fee", 5))))
                .put("/delivery/settings")
                .then()
                .statusCode(200);
    }

    // Número único por teste: o cache de coordenadas é compartilhado entre os testes.
    private static Map<String, Object> address(String street) {
        return Map.of(
                "streetAddress",
                street,
                "number",
                String.valueOf(System.nanoTime()),
                "cep",
                "13560-042");
    }

    private static List<Map<String, Object>> pastel() {
        long product = TestFixtures.createProduct("Pastel da entrega");
        return List.of(
                Map.of(
                        "type",
                        "PRODUCT",
                        "productSizeId",
                        TestFixtures.firstSizeOf(product),
                        "quantity",
                        1));
    }

    private static Map<String, Object> guestOrder(String fulfillment, Map<String, Object> address) {
        Map<String, Object> body = new HashMap<>();
        body.put("items", pastel());
        body.put("fulfillmentType", fulfillment);
        body.put("paymentMethod", "CASH");
        body.put("guestName", "Maria");
        body.put("guestPhone", "11999990000");
        body.put("guestEmail", "maria@example.com");
        if (address != null) {
            body.put("address", address);
        }
        return body;
    }

    private static ValidatableResponse quote(
            RequestSpecification spec, String fulfillment, Map<String, Object> address) {
        Map<String, Object> body = new HashMap<>();
        body.put("items", pastel());
        body.put("fulfillmentType", fulfillment);
        if (address != null) {
            body.put("address", address);
        }
        return spec.contentType(ContentType.JSON).body(body).post("/orders/quote").then();
    }

    private static ValidatableResponse place(RequestSpecification spec, Map<String, Object> body) {
        return spec.contentType(ContentType.JSON).body(body).post("/orders").then();
    }

    private record Client(long userId, String token) {}

    private static Client newClient() {
        String email = "cliente.entrega." + System.nanoTime() + "@example.com";
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

    @Test
    void theCepFillsInTheAddress() {
        given().get("/addresses/cep/{cep}", "13560-042")
                .then()
                .statusCode(200)
                .body("cep", is("13560-042"))
                .body("street", is("Rua do CEP"))
                .body("neighborhood", is("Centro"))
                .body("city", is("São Carlos"))
                .body("state", is("SP"));
        given().get("/addresses/cep/{cep}", GeoStub.CEP_ONLY_BRASILAPI)
                .then()
                .statusCode(200)
                .body("neighborhood", is("Vila Reserva"));
        given().get("/addresses/cep/{cep}", GeoStub.CEP_VIACEP_DOWN)
                .then()
                .statusCode(200)
                .body("street", is("Rua da Reserva"));
        given().get("/addresses/cep/{cep}", GeoStub.CEP_UNKNOWN).then().statusCode(404);
        given().get("/addresses/cep/{cep}", GeoStub.CEP_ALL_DOWN)
                .then()
                .statusCode(502)
                .body("message", is("Address lookup unavailable, try again"));
        given().get("/addresses/cep/{cep}", "1234").then().statusCode(400);
    }

    @Test
    void theCepIsAskedOnlyOnceWhileCached() {
        String cep = "135" + String.valueOf(System.nanoTime()).substring(0, 5);
        given().get("/addresses/cep/{cep}", cep).then().statusCode(200);
        given().get("/addresses/cep/{cep}", cep).then().statusCode(200);
        assertEquals(1, GeoStub.viaCepCalls(cep));
    }

    @Test
    void aSavedAddressGetsNeighborhoodCityAndStateFromTheCep() {
        Client client = newClient();
        as(client.token())
                .contentType(ContentType.JSON)
                .body(Map.of("streetAddress", "Rua A", "number", "1", "cep", "13560042"))
                .post("/users/{id}/addresses", client.userId())
                .then()
                .statusCode(201)
                .body("neighborhood", is("Centro"))
                .body("city", is("São Carlos"))
                .body("state", is("SP"));
        as(client.token())
                .contentType(ContentType.JSON)
                .body(Map.of("streetAddress", "Rua A", "number", "1", "cep", "99999-999"))
                .post("/users/{id}/addresses", client.userId())
                .then()
                .statusCode(400)
                .body("message", is("cep 99999-999 was not found"));
    }

    @Test
    void deliveryIsFreeWhileTheStoreIsNotConfigured() {
        Map<String, Object> address = address("Rua Longe");
        place(given(), guestOrder("DELIVERY", address))
                .statusCode(201)
                .body("order.deliveryFee", is(0.0f))
                .body("order.deliveryDistanceKm", nullValue())
                .body("order.total", is(10.0f))
                .body("order.address.city", is("São Carlos"));
        assertEquals(0, GeoStub.googleCalls("Rua Longe, " + address.get("number")));
    }

    @Test
    void theQuoteAndTheCheckoutChargeTheTierOfTheDistance() {
        configure();
        Map<String, Object> address = address("Rua Media");
        quote(given(), "DELIVERY", address)
                .statusCode(200)
                .body("deliveryFee", is(8.0f))
                .body("deliveryDistanceKm", is(4.0f))
                .body("total", is(18.0f))
                // A taxa não rende pontos.
                .body("pointsToEarn", is(10));
        JsonPath order =
                place(given(), guestOrder("DELIVERY", address))
                        .statusCode(201)
                        .extract()
                        .jsonPath();
        assertEquals(8.0f, order.getFloat("order.deliveryFee"));
        assertEquals(4.0f, order.getFloat("order.deliveryDistanceKm"));
        assertEquals(18.0f, order.getFloat("order.total"));
        assertEquals("Centro", order.getString("order.address.neighborhood"));
        assertEquals("SP", order.getString("order.address.state"));
        // A cotação e o checkout do mesmo endereço chamam o Google uma vez só.
        assertEquals(1, GeoStub.googleCalls("Rua Media, " + address.get("number")));

        quote(given(), "DELIVERY", address("Rua Perto"))
                .statusCode(200)
                .body("deliveryFee", is(5.0f))
                .body("total", is(15.0f));
    }

    @Test
    void aDeliveryWithoutAnAddressIsQuotedWithoutTheFee() {
        configure();
        quote(given(), "DELIVERY", null)
                .statusCode(200)
                .body("deliveryFee", is(0.0f))
                .body("deliveryDistanceKm", nullValue());
        quote(given(), "PICKUP", null).statusCode(200).body("deliveryFee", is(0.0f));
        quote(given(), "PICKUP", address("Rua Perto"))
                .statusCode(400)
                .body("message", is("Only DELIVERY orders take an address"));
        place(given(), guestOrder("PICKUP", null))
                .statusCode(201)
                .body("order.deliveryFee", is(0.0f));
    }

    @Test
    void anAddressBeyondTheRadiusIsRefused() {
        configure();
        quote(given(), "DELIVERY", address("Rua Longe"))
                .statusCode(400)
                .body("message", containsString("km away and the delivery reaches 6.00 km"));
        place(given(), guestOrder("DELIVERY", address("Rua Longe"))).statusCode(400);
    }

    @Test
    void anAddressThatCannotBeLocatedIsRefused() {
        configure();
        for (String street : List.of("Rua Inexistente", "Rua Vaga", "Rua Outra Cidade")) {
            quote(given(), "DELIVERY", address(street))
                    .statusCode(400)
                    .body("message", containsString("could not be located"));
        }
        quote(
                        given(),
                        "DELIVERY",
                        Map.of("streetAddress", "Rua A", "number", "1", "cep", "99999999"))
                .statusCode(400)
                .body("message", is("cep 99999999 was not found"));
    }

    @Test
    void aGeocodingFailureRefusesTheDelivery() {
        configure();
        quote(given(), "DELIVERY", address("Rua Cota"))
                .statusCode(400)
                .body("message", containsString("could not be calculated now"));
    }

    @Test
    void aClientQuotesWithASavedAddress() {
        configure();
        Client client = newClient();
        long addressId =
                as(client.token())
                        .contentType(ContentType.JSON)
                        .body(address("Rua Media"))
                        .post("/users/{id}/addresses", client.userId())
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("id");
        Map<String, Object> body = new HashMap<>();
        body.put("items", pastel());
        body.put("fulfillmentType", "DELIVERY");
        body.put("addressId", addressId);
        as(client.token())
                .contentType(ContentType.JSON)
                .body(body)
                .post("/orders/quote")
                .then()
                .statusCode(200)
                .body("deliveryFee", is(8.0f));
        given().contentType(ContentType.JSON)
                .body(body)
                .post("/orders/quote")
                .then()
                .statusCode(400)
                .body("message", is("addressId requires a client login"));

        body.put("paymentMethod", "CASH");
        place(as(client.token()), body).statusCode(201).body("order.deliveryFee", is(8.0f));
    }

    @Test
    void editingTheItemsKeepsTheDeliveryFee() {
        configure();
        long id =
                place(given(), guestOrder("DELIVERY", address("Rua Media")))
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("order.id");
        long product = TestFixtures.createProduct("Pastel editado");
        asAdmin()
                .contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "items",
                                List.of(
                                        Map.of(
                                                "type",
                                                "PRODUCT",
                                                "productSizeId",
                                                TestFixtures.firstSizeOf(product),
                                                "quantity",
                                                2))))
                .put("/orders/{id}/items", id)
                .then()
                .statusCode(200)
                .body("deliveryFee", is(8.0f))
                .body("total", is(28.0f));
    }

    @Test
    void settingsAreValidatedAndPublic() {
        given().get("/delivery/settings")
                .then()
                .statusCode(200)
                .body("enabled", is(false))
                .body("tiers", hasSize(0));
        configure();
        given().get("/delivery/settings")
                .then()
                .statusCode(200)
                .body("enabled", is(true))
                .body("tiers[0].maxDistanceKm", is(3.0f))
                .body("tiers[1].maxDistanceKm", is(6.0f))
                .body("maxDistanceKm", is(6.0f));

        for (Map<String, Object> invalid :
                List.of(
                        Map.<String, Object>of(
                                "storeLatitude",
                                100,
                                "storeLongitude",
                                0,
                                "tiers",
                                List.of(Map.of("maxDistanceKm", 3, "fee", 5))),
                        Map.<String, Object>of(
                                "storeLatitude", 0, "storeLongitude", 0, "tiers", List.of()),
                        Map.<String, Object>of(
                                "storeLatitude",
                                0,
                                "storeLongitude",
                                0,
                                "tiers",
                                List.of(Map.of("maxDistanceKm", 3, "fee", -1))))) {
            asAdmin()
                    .contentType(ContentType.JSON)
                    .body(invalid)
                    .put("/delivery/settings")
                    .then()
                    .statusCode(400)
                    .body("message", is("Invalid request"));
        }
        asAdmin()
                .contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "storeLatitude", 0,
                                "storeLongitude", 0,
                                "tiers",
                                        List.of(
                                                Map.of("maxDistanceKm", 3, "fee", 5),
                                                Map.of("maxDistanceKm", 3.0, "fee", 6))))
                .put("/delivery/settings")
                .then()
                .statusCode(400)
                .body("message", containsString("Two tiers share maxDistanceKm"));
    }

    @Test
    void expiredCoordinatesAreDeletedAndAskedAgain() {
        configure();
        Map<String, Object> address = address("Rua Perto");
        String search = "Rua Perto, " + address.get("number");
        quote(given(), "DELIVERY", address).statusCode(200);
        assertEquals(1, GeoStub.googleCalls(search));

        clock.advance(Duration.ofDays(30));
        assertTrue(geocodingService.purgeExpired() >= 1);
        quote(given(), "DELIVERY", address).statusCode(200);
        assertEquals(2, GeoStub.googleCalls(search));
    }
}

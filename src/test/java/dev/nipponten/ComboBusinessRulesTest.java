package dev.nipponten;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ComboBusinessRulesTest {

    private Map<String, Object> combo() {
        return Map.of("name", "Combo", "price", 30.0, "status", "ACTIVE");
    }

    @Test
    void rejectsComboWithASingleProduct() {
        long product = TestFixtures.createProduct("Pastel");

        given().contentType(ContentType.JSON)
                .body(Map.of("combo", combo(), "products", List.of(Map.of("productId", product))))
                .post("/combos")
                .then()
                .statusCode(400);
    }

    @Test
    void createsComboWithTwoProducts() {
        long first = TestFixtures.createProduct("Pastel de carne");
        long second = TestFixtures.createProduct("Refrigerante");

        long comboId =
                given().contentType(ContentType.JSON)
                        .body(
                                Map.of(
                                        "combo",
                                        combo(),
                                        "products",
                                        List.of(
                                                Map.of("productId", first),
                                                Map.of("productId", second))))
                        .post("/combos")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("id");

        given().get("/combos/{id}/products", comboId)
                .then()
                .statusCode(200)
                .body("size()", org.hamcrest.Matchers.is(2));
    }

    @Test
    void refusesToDropBelowTwoProducts() {
        long first = TestFixtures.createProduct("Pastel de queijo");
        long second = TestFixtures.createProduct("Suco");

        long comboId =
                given().contentType(ContentType.JSON)
                        .body(
                                Map.of(
                                        "combo",
                                        combo(),
                                        "products",
                                        List.of(
                                                Map.of("productId", first),
                                                Map.of("productId", second))))
                        .post("/combos")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("id");

        long comboProductId =
                given().get("/combos/{id}/products", comboId)
                        .then()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getLong("[0].id");

        given().delete("/combos/{comboId}/products/{id}", comboId, comboProductId)
                .then()
                .statusCode(400);
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        long first = TestFixtures.createProduct("Pastel de palmito");
        long second = TestFixtures.createProduct("Agua");

        given().contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "combo",
                                Map.of(
                                        "name",
                                        "Combo",
                                        "price",
                                        30.0,
                                        "status",
                                        "ACTIVE",
                                        "startDate",
                                        "2026-10-10T10:00:00",
                                        "endDate",
                                        "2026-10-01T10:00:00"),
                                "products",
                                List.of(Map.of("productId", first), Map.of("productId", second))))
                .post("/combos")
                .then()
                .statusCode(400);
    }
}

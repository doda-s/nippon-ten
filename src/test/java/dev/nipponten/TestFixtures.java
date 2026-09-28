package dev.nipponten;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import java.util.Map;

/** Cria, via API, os agregados de apoio que os testes de regra de negócio precisam. */
final class TestFixtures {

    private TestFixtures() {}

    static long createProduct(String name) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("name", name, "description", "d", "status", "ACTIVE"))
                .post("/products")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");
    }

    static long createSize(String name) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("name", name))
                .post("/sizes")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");
    }

    static long createIngredient(String name) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("name", name, "price", 1.5, "status", "ACTIVE"))
                .post("/ingredients")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");
    }

    static long createPromotionType(String name) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("name", name, "type", "PERCENTAGE_DISCOUNT", "value", 10))
                .post("/promotion-types")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");
    }
}

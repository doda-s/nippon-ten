package dev.nipponten;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;

/** Cria, via API, os agregados de apoio que os testes de regra de negócio precisam. */
final class TestFixtures {

    private TestFixtures() {}

    // Todo produto nasce com ao menos um tamanho (é onde fica o preço).
    static long createProduct(String name) {
        long size = createSize("Unico-" + System.nanoTime());
        return given().contentType(ContentType.JSON)
                .body(productRegistration(name, List.of(), List.of(sizeWithPrice(size, 10.0))))
                .post("/products")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");
    }

    static Map<String, Object> productRegistration(
            String name, List<Long> ingredientIds, List<Map<String, Object>> sizes) {
        return Map.of(
                "product",
                Map.of("name", name, "description", "d", "status", "ACTIVE"),
                "ingredients",
                ingredientIds.stream().map(id -> Map.of("ingredientId", id)).toList(),
                "sizes",
                sizes);
    }

    static Map<String, Object> sizeWithPrice(long sizeId, double price) {
        return Map.of("sizeId", sizeId, "price", price, "status", "ACTIVE");
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

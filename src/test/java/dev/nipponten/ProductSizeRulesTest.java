package dev.nipponten;

import static dev.nipponten.TestAuth.asAdmin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProductSizeRulesTest {

    @Test
    void rejectsUnknownSizeWithNotFound() {
        long product = TestFixtures.createProduct("Pastel sem tamanho");

        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("sizeId", 999999, "price", 10.0, "status", "ACTIVE"))
                .post("/products/{id}/sizes", product)
                .then()
                .statusCode(404);
    }

    @Test
    void rejectsTheSameSizeTwiceOnTheSameProduct() {
        long product = TestFixtures.createProduct("Pastel duplicado");
        long size = TestFixtures.createSize("Medio-" + System.nanoTime());
        Map<String, Object> body = Map.of("sizeId", size, "price", 10.0, "status", "ACTIVE");

        asAdmin()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/products/{id}/sizes", product)
                .then()
                .statusCode(201);

        asAdmin()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/products/{id}/sizes", product)
                .then()
                .statusCode(400);
    }

    @Test
    void refusesToDeleteASizeStillInUse() {
        long product = TestFixtures.createProduct("Pastel com tamanho");
        long size = TestFixtures.createSize("Pequeno-" + System.nanoTime());

        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("sizeId", size, "price", 10.0, "status", "ACTIVE"))
                .post("/products/{id}/sizes", product)
                .then()
                .statusCode(201);

        asAdmin().delete("/sizes/{id}", size).then().statusCode(400);
    }

    @Test
    void reusesTheSameSizeAcrossProductsWithDifferentPrices() {
        long first = TestFixtures.createProduct("Pastel A");
        long second = TestFixtures.createProduct("Pastel B");
        long size = TestFixtures.createSize("Familia-" + System.nanoTime());

        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("sizeId", size, "price", 10.0, "status", "ACTIVE"))
                .post("/products/{id}/sizes", first)
                .then()
                .statusCode(201);

        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("sizeId", size, "price", 25.0, "status", "ACTIVE"))
                .post("/products/{id}/sizes", second)
                .then()
                .statusCode(201);
    }
}

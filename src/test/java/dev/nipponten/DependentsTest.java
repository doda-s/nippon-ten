package dev.nipponten;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DependentsTest {

    @Test
    void listsProductsDependingOnAnIngredientWithoutChangingTheirStatus() {
        long product = TestFixtures.createProduct("Pastel de carne seca");
        long ingredient = TestFixtures.createIngredient("Carne seca-" + System.nanoTime());

        given().contentType(ContentType.JSON)
                .body(Map.of("ingredientId", ingredient))
                .post("/products/{id}/ingredients", product)
                .then()
                .statusCode(201);

        // Marcar como esgotado não propaga status: só devolve quem depende, para decisão manual.
        given().contentType(ContentType.JSON)
                .body(Map.of("name", "Carne seca", "price", 1.5, "status", "OUT_OF_STOCK"))
                .put("/ingredients/{id}", ingredient)
                .then()
                .statusCode(200);

        given().get("/ingredients/{id}/dependents", ingredient)
                .then()
                .statusCode(200)
                .body("products.id", hasItem((int) product));

        given().get("/products/{id}", product).then().statusCode(200).body("status", is("ACTIVE"));
    }

    @Test
    void listsCombosDependingOnAProduct() {
        long first = TestFixtures.createProduct("Pastel dependente");
        long second = TestFixtures.createProduct("Bebida dependente");

        long comboId =
                given().contentType(ContentType.JSON)
                        .body(
                                Map.of(
                                        "combo",
                                        Map.of(
                                                "name",
                                                "Combo dep",
                                                "price",
                                                20.0,
                                                "status",
                                                "ACTIVE"),
                                        "products",
                                        java.util.List.of(
                                                Map.of("productId", first),
                                                Map.of("productId", second))))
                        .post("/combos")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("id");

        given().get("/products/{id}/dependents", first)
                .then()
                .statusCode(200)
                .body("combos.id", hasItem((int) comboId));
    }
}

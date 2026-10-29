package dev.nipponten;

import static dev.nipponten.TestAuth.asAdmin;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DependentsTest {

    @Test
    void listsProductsDependingOnAnIngredientWithoutChangingTheirStatus() {
        long product = TestFixtures.createProduct("Pastel de carne seca");
        long ingredient = TestFixtures.createIngredient("Carne seca-" + System.nanoTime());

        asAdmin()
                .contentType(ContentType.JSON)
                .body(List.of(Map.of("ingredientId", ingredient)))
                .post("/products/{id}/ingredients", product)
                .then()
                .statusCode(201);

        // Marcar como esgotado não propaga status: só devolve quem depende, para decisão manual.
        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Carne seca", "price", 1.5, "status", "OUT_OF_STOCK"))
                .put("/ingredients/{id}", ingredient)
                .then()
                .statusCode(200);

        asAdmin()
                .get("/ingredients/{id}/dependents", ingredient)
                .then()
                .statusCode(200)
                .body("products.id", hasItem((int) product));

        asAdmin()
                .get("/products/{id}", product)
                .then()
                .statusCode(200)
                .body("status", is("ACTIVE"));
    }

    @Test
    void listsCombosDependingOnAProduct() {
        long first = TestFixtures.createProduct("Pastel dependente");
        long second = TestFixtures.createProduct("Bebida dependente");

        long comboId =
                asAdmin()
                        .contentType(ContentType.JSON)
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
                                                TestFixtures.comboItem(first, 1),
                                                TestFixtures.comboItem(second, 1))))
                        .post("/combos")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("id");

        asAdmin()
                .get("/products/{id}/dependents", first)
                .then()
                .statusCode(200)
                .body("combos.id", hasItem((int) comboId));
    }
}

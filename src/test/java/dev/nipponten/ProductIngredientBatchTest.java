package dev.nipponten;

import static dev.nipponten.TestAuth.asAdmin;
import static org.hamcrest.Matchers.hasSize;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProductIngredientBatchTest {

    @Test
    void addsSeveralIngredientsToAProductInOneRequest() {
        long product = TestFixtures.createProduct("Combinado");
        long rice = TestFixtures.createIngredient("Arroz-" + System.nanoTime());
        long salmon = TestFixtures.createIngredient("Salmao-" + System.nanoTime());

        asAdmin()
                .contentType(ContentType.JSON)
                .body(List.of(Map.of("ingredientId", rice), Map.of("ingredientId", salmon)))
                .post("/products/{id}/ingredients", product)
                .then()
                .statusCode(201)
                .body("$", hasSize(2));
    }

    @Test
    void rejectsTheSameIngredientTwiceInTheSameBatch() {
        long product = TestFixtures.createProduct("Combinado duplicado");
        long ingredient = TestFixtures.createIngredient("Repetido-" + System.nanoTime());

        asAdmin()
                .contentType(ContentType.JSON)
                .body(
                        List.of(
                                Map.of("ingredientId", ingredient),
                                Map.of("ingredientId", ingredient)))
                .post("/products/{id}/ingredients", product)
                .then()
                .statusCode(400);
    }

    @Test
    void createsNothingWhenAnIngredientInTheBatchDoesNotExist() {
        long product = TestFixtures.createProduct("Combinado atomico");
        long ingredient = TestFixtures.createIngredient("Existente-" + System.nanoTime());

        asAdmin()
                .contentType(ContentType.JSON)
                .body(List.of(Map.of("ingredientId", ingredient), Map.of("ingredientId", 999999)))
                .post("/products/{id}/ingredients", product)
                .then()
                .statusCode(404);

        asAdmin().get("/products/{id}/ingredients", product).then().body("$", hasSize(0));
    }

    @Test
    void addsSeveralAdditionalIngredientsToAProductInOneRequest() {
        long product = TestFixtures.createProduct("Temaki adicional");
        long nori = TestFixtures.createIngredient("Nori-" + System.nanoTime());
        long gergelim = TestFixtures.createIngredient("Gergelim-" + System.nanoTime());

        asAdmin()
                .contentType(ContentType.JSON)
                .body(
                        List.of(
                                Map.of(
                                        "ingredientId",
                                        nori,
                                        "maximumQuantity",
                                        2,
                                        "status",
                                        "ACTIVE"),
                                Map.of(
                                        "ingredientId",
                                        gergelim,
                                        "maximumQuantity",
                                        3,
                                        "status",
                                        "ACTIVE")))
                .post("/products/{id}/additional-ingredients", product)
                .then()
                .statusCode(201)
                .body("$", hasSize(2));
    }
}

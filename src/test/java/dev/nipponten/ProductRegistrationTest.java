package dev.nipponten;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProductRegistrationTest {

    @Test
    void createsProductWithIngredientsAndPricedSizesAtOnce() {
        long rice = TestFixtures.createIngredient("Arroz-" + System.nanoTime());
        long salmon = TestFixtures.createIngredient("Salmao-" + System.nanoTime());
        long small = TestFixtures.createSize("P-" + System.nanoTime());
        long large = TestFixtures.createSize("G-" + System.nanoTime());

        long product =
                given().contentType(ContentType.JSON)
                        .body(
                                TestFixtures.productRegistration(
                                        "Temaki",
                                        List.of(rice, salmon),
                                        List.of(
                                                TestFixtures.sizeWithPrice(small, 29.9),
                                                TestFixtures.sizeWithPrice(large, 36.9))))
                        .post("/products")
                        .then()
                        .statusCode(201)
                        .body("ingredients", hasSize(2))
                        .body("sizes", hasSize(2))
                        .extract()
                        .jsonPath()
                        .getLong("id");

        given().get("/products/{id}", product)
                .then()
                .statusCode(200)
                .body("ingredients.ingredientId", containsInAnyOrder((int) rice, (int) salmon))
                .body("sizes.sizeId", containsInAnyOrder((int) small, (int) large))
                .body("sizes.find { it.sizeId == %s }.price".formatted(small), is(29.9f));
    }

    @Test
    void ingredientsAreOptional() {
        long size = TestFixtures.createSize("U-" + System.nanoTime());

        given().contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "product",
                                Map.of("name", "Agua", "status", "ACTIVE"),
                                "sizes",
                                List.of(TestFixtures.sizeWithPrice(size, 5.0))))
                .post("/products")
                .then()
                .statusCode(201)
                .body("ingredients", hasSize(0));
    }

    @Test
    void rejectsProductWithoutSizes() {
        given().contentType(ContentType.JSON)
                .body(TestFixtures.productRegistration("Sem preco", List.of(), List.of()))
                .post("/products")
                .then()
                .statusCode(400);
    }

    @Test
    void rejectsTheSameSizeTwiceInTheRequest() {
        long size = TestFixtures.createSize("Dup-" + System.nanoTime());

        given().contentType(ContentType.JSON)
                .body(
                        TestFixtures.productRegistration(
                                "Duplicado",
                                List.of(),
                                List.of(
                                        TestFixtures.sizeWithPrice(size, 10.0),
                                        TestFixtures.sizeWithPrice(size, 12.0))))
                .post("/products")
                .then()
                .statusCode(400);
    }

    @Test
    void rejectsTheSameIngredientTwiceInTheRequest() {
        long ingredient = TestFixtures.createIngredient("Dup-" + System.nanoTime());
        long size = TestFixtures.createSize("DupI-" + System.nanoTime());

        given().contentType(ContentType.JSON)
                .body(
                        TestFixtures.productRegistration(
                                "Duplicado",
                                List.of(ingredient, ingredient),
                                List.of(TestFixtures.sizeWithPrice(size, 10.0))))
                .post("/products")
                .then()
                .statusCode(400);
    }

    @Test
    void createsNothingWhenAnIngredientDoesNotExist() {
        long size = TestFixtures.createSize("Atomic-" + System.nanoTime());
        String name = "Atomico-" + System.nanoTime();

        given().contentType(ContentType.JSON)
                .body(
                        TestFixtures.productRegistration(
                                name,
                                List.of(999999L),
                                List.of(TestFixtures.sizeWithPrice(size, 10.0))))
                .post("/products")
                .then()
                .statusCode(404);

        given().get("/products")
                .then()
                .body("findAll { it.name == '%s' }".formatted(name), hasSize(0));
        // O tamanho não ficou associado a nenhum produto, então pode ser removido.
        given().delete("/sizes/{id}", size).then().statusCode(204);
    }

    @Test
    void refusesToRemoveTheLastSizeOfAProduct() {
        long product = TestFixtures.createProduct("Ultimo tamanho");
        long productSize =
                given().get("/products/{id}/sizes", product).jsonPath().getLong("[0].id");

        given().delete("/products/{productId}/sizes/{id}", product, productSize)
                .then()
                .statusCode(400);
    }
}

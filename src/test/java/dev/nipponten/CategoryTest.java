package dev.nipponten;

import static dev.nipponten.TestAuth.asAdmin;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CategoryTest {

    private static long createCategory(String name, Integer displayOrder) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("displayOrder", displayOrder);
        return asAdmin()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/categories")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");
    }

    private static long createProductIn(Long categoryId, String name) {
        Map<String, Object> product = new HashMap<>();
        product.put("name", name);
        product.put("status", "ACTIVE");
        product.put("categoryId", categoryId);
        return asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("product", product))
                .post("/products")
                .then()
                .statusCode(201)
                .body("categoryId", categoryId == null ? nullValue() : is(categoryId.intValue()))
                .extract()
                .jsonPath()
                .getLong("id");
    }

    private static Map<String, Object> productBody(String name, Object categoryId) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("status", "ACTIVE");
        body.put("categoryId", categoryId);
        return body;
    }

    @Test
    void listsCategoriesPubliclyInDisplayOrder() {
        String suffix = "-" + System.nanoTime();
        long sucos = createCategory("Sucos" + suffix, 900_002);
        long pasteis = createCategory("Pasteis" + suffix, 900_001);

        // A última página traz as recém-criadas, com a ordem de exibição maior que a das outras.
        int total = given().get("/categories?size=100").then().extract().path("totalElements");
        given().get("/categories?size=100&page=" + ((total - 1) / 100))
                .then()
                .statusCode(200)
                .body(
                        "content.findAll { it.name.endsWith('%s') }.id".formatted(suffix),
                        contains((int) pasteis, (int) sucos));
    }

    @Test
    void displayOrderDefaultsToZeroAndNameIsTrimmed() {
        String name = "Pizzas-" + System.nanoTime();
        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "  " + name + "  "))
                .post("/categories")
                .then()
                .statusCode(201)
                .body("name", is(name))
                .body("displayOrder", is(0));
    }

    @Test
    void refusesARepeatedNameIgnoringCase() {
        String name = "Bebidas-" + System.nanoTime();
        long id = createCategory(name, 1);

        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name.toUpperCase()))
                .post("/categories")
                .then()
                .statusCode(400);

        // Renomear a própria categoria para o mesmo nome continua valendo.
        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name, "displayOrder", 5))
                .put("/categories/{id}", id)
                .then()
                .statusCode(200)
                .body("displayOrder", is(5));
    }

    @Test
    void validatesTheName() {
        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("name", " "))
                .post("/categories")
                .then()
                .statusCode(400)
                .body("violations.field", hasItem("name"));
        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "x".repeat(61)))
                .post("/categories")
                .then()
                .statusCode(400);
    }

    @Test
    void productKeepsItsCategoryUntilItIsTakenOut() {
        long category = createCategory("Pasteis doces-" + System.nanoTime(), 3);
        long product = createProductIn(category, "Pastel de banana");
        createProductIn(null, "Sem categoria");

        given().get("/products/{id}", product)
                .then()
                .statusCode(200)
                .body("categoryId", is((int) category));

        // Tirar a categoria do produto.
        asAdmin()
                .contentType(ContentType.JSON)
                .body(productBody("Pastel de banana", null))
                .put("/products/{id}", product)
                .then()
                .statusCode(200)
                .body("categoryId", nullValue());
    }

    @Test
    void unknownCategoryIsNotFound() {
        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("product", productBody("Fantasma", 999_999)))
                .post("/products")
                .then()
                .statusCode(404);

        long product = createProductIn(null, "Pastel de vento");
        asAdmin()
                .contentType(ContentType.JSON)
                .body(productBody("Pastel de vento", 999_999))
                .put("/products/{id}", product)
                .then()
                .statusCode(404);

        given().get("/categories/999999").then().statusCode(404);
    }

    @Test
    void aCategoryInUseCannotBeDeleted() {
        long category = createCategory("Lanches-" + System.nanoTime(), 4);
        long product = createProductIn(category, "Misto quente");

        asAdmin().delete("/categories/{id}", category).then().statusCode(400);

        asAdmin()
                .contentType(ContentType.JSON)
                .body(productBody("Misto quente", null))
                .put("/products/{id}", product)
                .then()
                .statusCode(200);
        asAdmin().delete("/categories/{id}", category).then().statusCode(204);
        given().get("/categories/{id}", category).then().statusCode(404);
    }
}

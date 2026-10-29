package dev.nipponten;

import static dev.nipponten.TestAuth.asAdmin;
import static dev.nipponten.TestFixtures.comboItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ComboBusinessRulesTest {

    private Map<String, Object> combo() {
        return Map.of("name", "Combo", "price", 30.0, "status", "ACTIVE");
    }

    private Map<String, Object> registration(List<Map<String, Object>> products) {
        return Map.of("combo", combo(), "products", products);
    }

    private long createCombo(List<Map<String, Object>> products) {
        return asAdmin()
                .contentType(ContentType.JSON)
                .body(registration(products))
                .post("/combos")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");
    }

    private long firstComboProductId(long comboId) {
        return asAdmin()
                .get("/combos/{id}/products", comboId)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getLong("[0].id");
    }

    // Map.of não aceita valor nulo: o tamanho fica de fora do corpo.
    private Map<String, Object> itemWithoutSize(long productId, int quantity) {
        Map<String, Object> item = new HashMap<>();
        item.put("productId", productId);
        item.put("quantity", quantity);
        return item;
    }

    @Test
    void rejectsComboWithASingleProduct() {
        long product = TestFixtures.createProduct("Pastel");

        asAdmin()
                .contentType(ContentType.JSON)
                .body(registration(List.of(comboItem(product, 1))))
                .post("/combos")
                .then()
                .statusCode(400);
    }

    @Test
    void createsComboWithTwoProducts() {
        long first = TestFixtures.createProduct("Pastel de carne");
        long second = TestFixtures.createProduct("Refrigerante");

        long comboId = createCombo(List.of(comboItem(first, 1), comboItem(second, 1)));

        asAdmin()
                .get("/combos/{id}/products", comboId)
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("[0].productId", is((int) first))
                .body("[0].productSizeId", is((int) TestFixtures.firstSizeOf(first)))
                .body("[0].quantity", is(1));
    }

    @Test
    void countsQuantitiesTowardsTheMinimumOfTwoProducts() {
        long product = TestFixtures.createProduct("Pastel duplo");

        long comboId = createCombo(List.of(comboItem(product, 2)));

        asAdmin()
                .get("/combos/{id}/products", comboId)
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].quantity", is(2));
    }

    @Test
    void requiresTheSizeWhenTheProductHasSizes() {
        long first = TestFixtures.createProduct("Pastel sem tamanho escolhido");
        long second = TestFixtures.createProduct("Suco de laranja");

        asAdmin()
                .contentType(ContentType.JSON)
                .body(registration(List.of(itemWithoutSize(first, 1), comboItem(second, 1))))
                .post("/combos")
                .then()
                .statusCode(400);
    }

    @Test
    void acceptsAProductWithoutSizesWithoutChoosingOne() {
        long sauce = TestFixtures.createProductWithoutSizes("Molho da casa");
        long pastel = TestFixtures.createProduct("Pastel com molho");

        long comboId = createCombo(List.of(itemWithoutSize(sauce, 1), comboItem(pastel, 1)));

        asAdmin()
                .get("/combos/{id}/products", comboId)
                .then()
                .statusCode(200)
                .body("find { it.productId == " + sauce + " }.productSizeId", nullValue());
    }

    @Test
    void rejectsASizeFromAnotherProduct() {
        long first = TestFixtures.createProduct("Pastel de frango");
        long second = TestFixtures.createProduct("Cha gelado");
        Map<String, Object> wrongSize =
                Map.of(
                        "productId",
                        first,
                        "productSizeId",
                        TestFixtures.firstSizeOf(second),
                        "quantity",
                        1);

        asAdmin()
                .contentType(ContentType.JSON)
                .body(registration(List.of(wrongSize, comboItem(second, 1))))
                .post("/combos")
                .then()
                .statusCode(404);
    }

    @Test
    void rejectsTheSameProductSizeTwiceInTheRegistration() {
        long product = TestFixtures.createProduct("Pastel repetido");

        asAdmin()
                .contentType(ContentType.JSON)
                .body(registration(List.of(comboItem(product, 1), comboItem(product, 1))))
                .post("/combos")
                .then()
                .statusCode(400);
    }

    @Test
    void rejectsAddingAProductSizeAlreadyInTheCombo() {
        long first = TestFixtures.createProduct("Pastel de pizza");
        long second = TestFixtures.createProduct("Guarana");
        long comboId = createCombo(List.of(comboItem(first, 1), comboItem(second, 1)));

        asAdmin()
                .contentType(ContentType.JSON)
                .body(comboItem(first, 1))
                .post("/combos/{id}/products", comboId)
                .then()
                .statusCode(400);
    }

    @Test
    void refusesToDropBelowTwoProducts() {
        long first = TestFixtures.createProduct("Pastel de queijo");
        long second = TestFixtures.createProduct("Suco");
        long comboId = createCombo(List.of(comboItem(first, 1), comboItem(second, 1)));

        asAdmin()
                .delete("/combos/{comboId}/products/{id}", comboId, firstComboProductId(comboId))
                .then()
                .statusCode(400);
    }

    @Test
    void refusesToLowerTheQuantityBelowTwoProducts() {
        long product = TestFixtures.createProduct("Pastel de banana");
        long comboId = createCombo(List.of(comboItem(product, 2)));

        asAdmin()
                .contentType(ContentType.JSON)
                .body(comboItem(product, 1))
                .put("/combos/{comboId}/products/{id}", comboId, firstComboProductId(comboId))
                .then()
                .statusCode(400);
    }

    @Test
    void refusesToRemoveAProductSizeUsedByACombo() {
        long product = TestFixtures.createProduct("Pastel de dois tamanhos");
        long sizeInCombo = TestFixtures.firstSizeOf(product);
        // Um segundo tamanho, para a regra "o produto mantém ao menos um tamanho" não responder
        // antes.
        long otherSize = TestFixtures.createSize("Grande-" + System.nanoTime());
        asAdmin()
                .contentType(ContentType.JSON)
                .body(TestFixtures.sizeWithPrice(otherSize, 15.0))
                .post("/products/{id}/sizes", product)
                .then()
                .statusCode(201);
        createCombo(List.of(comboItem(product, 2)));

        asAdmin()
                .delete("/products/{productId}/sizes/{id}", product, sizeInCombo)
                .then()
                .statusCode(400);
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        long first = TestFixtures.createProduct("Pastel de palmito");
        long second = TestFixtures.createProduct("Agua");

        asAdmin()
                .contentType(ContentType.JSON)
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
                                List.of(comboItem(first, 1), comboItem(second, 1))))
                .post("/combos")
                .then()
                .statusCode(400);
    }
}

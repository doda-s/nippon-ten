package dev.nipponten;

import static dev.nipponten.TestAuth.asAdmin;
import static dev.nipponten.TestFixtures.comboItem;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OrderQuoteRulesTest {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    private ValidatableResponse quote(List<Map<String, Object>> items) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("items", items))
                .post("/orders/quote")
                .then();
    }

    private Map<String, Object> productItem(long productSizeId, int quantity) {
        Map<String, Object> item = new HashMap<>();
        item.put("type", "PRODUCT");
        item.put("productSizeId", productSizeId);
        item.put("quantity", quantity);
        return item;
    }

    private void offerAdditional(long product, long ingredient, int maximum, String status) {
        asAdmin()
                .contentType(ContentType.JSON)
                .body(
                        List.of(
                                Map.of(
                                        "ingredientId",
                                        ingredient,
                                        "maximumQuantity",
                                        maximum,
                                        "status",
                                        status)))
                .post("/products/{id}/additional-ingredients", product)
                .then()
                .statusCode(201);
    }

    private void addToComposition(long product, long ingredient) {
        asAdmin()
                .contentType(ContentType.JSON)
                .body(List.of(Map.of("ingredientId", ingredient)))
                .post("/products/{id}/ingredients", product)
                .then()
                .statusCode(201);
    }

    private void markOutOfStock(long ingredient, String name) {
        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name, "price", 1.5, "status", "OUT_OF_STOCK"))
                .put("/ingredients/{id}", ingredient)
                .then()
                .statusCode(200);
    }

    private void createPromotion(long product, LocalDateTime start, LocalDateTime end) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Promo");
        body.put("status", "ACTIVE");
        body.put("promotionTypeId", TestFixtures.createPromotionType("Dez-" + System.nanoTime()));
        body.put("productId", product);
        body.put("startDate", start.truncatedTo(ChronoUnit.SECONDS).toString());
        body.put("endDate", end.truncatedTo(ChronoUnit.SECONDS).toString());
        asAdmin()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/promotions")
                .then()
                .statusCode(201);
    }

    @Test
    void isPublicAndPricesAProductBySizeWithAdditionals() {
        long product = TestFixtures.createProduct("Pastel cotado");
        long cheese = TestFixtures.createIngredient("Queijo extra-" + System.nanoTime());
        offerAdditional(product, cheese, 3, "ACTIVE");
        Map<String, Object> item = productItem(TestFixtures.firstSizeOf(product), 2);
        item.put("additionals", List.of(Map.of("ingredientId", cheese, "quantity", 2)));
        item.put("notes", "bem passado");

        // (10,00 + 2 x 1,50) x 2 = 26,00
        quote(List.of(item))
                .statusCode(200)
                .body("items[0].name", is("Pastel cotado"))
                .body("items[0].unitPrice", is(10.0f))
                .body("items[0].additionals[0].unitPrice", is(1.5f))
                .body("items[0].total", is(26.0f))
                .body("items[0].notes", is("bem passado"))
                .body("subtotal", is(26.0f))
                .body("discount", is(0.0f))
                .body("deliveryFee", is(0.0f))
                .body("total", is(26.0f));
    }

    @Test
    void appliesAnActivePromotionAutomatically() {
        long product = TestFixtures.createProduct("Pastel em promocao");
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        createPromotion(product, now.minusDays(1), now.plusDays(1));

        quote(List.of(productItem(TestFixtures.firstSizeOf(product), 1)))
                .statusCode(200)
                .body("items[0].originalUnitPrice", is(10.0f))
                .body("items[0].unitPrice", is(9.0f))
                .body("items[0].promotionId", org.hamcrest.Matchers.notNullValue())
                .body("total", is(9.0f));
    }

    @Test
    void ignoresAPromotionOutsideItsWindow() {
        long product = TestFixtures.createProduct("Pastel promo futura");
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        createPromotion(product, now.plusDays(1), now.plusDays(2));

        quote(List.of(productItem(TestFixtures.firstSizeOf(product), 1)))
                .statusCode(200)
                .body("items[0].unitPrice", is(10.0f))
                .body("items[0].promotionId", nullValue());
    }

    @Test
    void pricesAComboByItsOwnPriceAndListsItsComponents() {
        long pastel = TestFixtures.createProduct("Pastel do combo");
        long drink = TestFixtures.createProduct("Refri do combo");
        long combo =
                asAdmin()
                        .contentType(ContentType.JSON)
                        .body(
                                Map.of(
                                        "combo",
                                        Map.of(
                                                "name",
                                                "Combo casal",
                                                "price",
                                                25.0,
                                                "status",
                                                "ACTIVE"),
                                        "products",
                                        List.of(comboItem(pastel, 2), comboItem(drink, 1))))
                        .post("/combos")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("id");

        quote(List.of(Map.of("type", "COMBO", "comboId", combo, "quantity", 2)))
                .statusCode(200)
                .body("items[0].name", is("Combo casal"))
                .body("items[0].unitPrice", is(25.0f))
                .body("items[0].total", is(50.0f))
                .body("items[0].comboComponents", hasSize(2))
                .body("items[0].comboComponents.name", hasItem("Pastel do combo"))
                .body("items[0].comboComponents.quantity", hasItem(2));
    }

    @Test
    void rejectsAnAdditionalAboveTheMaximum() {
        long product = TestFixtures.createProduct("Pastel com limite");
        long bacon = TestFixtures.createIngredient("Bacon-" + System.nanoTime());
        offerAdditional(product, bacon, 1, "ACTIVE");
        Map<String, Object> item = productItem(TestFixtures.firstSizeOf(product), 1);
        item.put("additionals", List.of(Map.of("ingredientId", bacon, "quantity", 2)));

        quote(List.of(item))
                .statusCode(400)
                .body("violations.field", hasItem("items[0].additionals[0]"));
    }

    @Test
    void rejectsAnAdditionalNotOfferedForTheProduct() {
        long product = TestFixtures.createProduct("Pastel sem adicionais");
        long olive = TestFixtures.createIngredient("Azeitona-" + System.nanoTime());
        Map<String, Object> item = productItem(TestFixtures.firstSizeOf(product), 1);
        item.put("additionals", List.of(Map.of("ingredientId", olive, "quantity", 1)));

        quote(List.of(item))
                .statusCode(400)
                .body("violations.field", hasItem("items[0].additionals[0]"));
    }

    @Test
    void reportsEveryUnavailableItemAtOnce() {
        long inactive = TestFixtures.createProduct("Pastel inativo");
        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Pastel inativo", "description", "d", "status", "INACTIVE"))
                .put("/products/{id}", inactive)
                .then()
                .statusCode(200);
        long product = TestFixtures.createProduct("Pastel com adicional esgotado");
        String name = "Catupiry-" + System.nanoTime();
        long catupiry = TestFixtures.createIngredient(name);
        offerAdditional(product, catupiry, 2, "ACTIVE");
        markOutOfStock(catupiry, name);
        Map<String, Object> withAdditional = productItem(TestFixtures.firstSizeOf(product), 1);
        withAdditional.put("additionals", List.of(Map.of("ingredientId", catupiry, "quantity", 1)));

        quote(List.of(productItem(TestFixtures.firstSizeOf(inactive), 1), withAdditional))
                .statusCode(400)
                .body("message", is("Some items are unavailable"))
                .body("violations", hasSize(2))
                .body("violations.field", hasItem("items[0]"))
                .body("violations.field", hasItem("items[1].additionals[0]"));
    }

    @Test
    void blocksAnOutOfStockIngredientUnlessTheCustomerRemovesIt() {
        long product = TestFixtures.createProduct("Pastel de palmito esgotado");
        String name = "Palmito-" + System.nanoTime();
        long palmito = TestFixtures.createIngredient(name);
        addToComposition(product, palmito);
        markOutOfStock(palmito, name);
        long size = TestFixtures.firstSizeOf(product);

        quote(List.of(productItem(size, 1)))
                .statusCode(400)
                .body("violations.field", hasItem("items[0]"));

        Map<String, Object> withoutPalmito = productItem(size, 1);
        withoutPalmito.put("removedIngredientIds", List.of(palmito));
        quote(List.of(withoutPalmito))
                .statusCode(200)
                .body("items[0].removedIngredients[0].ingredientId", is((int) palmito))
                .body("items[0].unitPrice", is(10.0f));
    }

    @Test
    void rejectsRemovingAnIngredientOutsideTheComposition() {
        long product = TestFixtures.createProduct("Pastel simples");
        long onion = TestFixtures.createIngredient("Cebola-" + System.nanoTime());
        Map<String, Object> item = productItem(TestFixtures.firstSizeOf(product), 1);
        item.put("removedIngredientIds", List.of(onion));

        quote(List.of(item))
                .statusCode(400)
                .body("violations.field", hasItem("items[0].removedIngredientIds[0]"));
    }

    @Test
    void rejectsAComboOutsideItsWindow() {
        long first = TestFixtures.createProduct("Pastel combo expirado");
        long second = TestFixtures.createProduct("Suco combo expirado");
        long combo =
                asAdmin()
                        .contentType(ContentType.JSON)
                        .body(
                                Map.of(
                                        "combo",
                                        Map.of(
                                                "name",
                                                "Combo expirado",
                                                "price",
                                                20.0,
                                                "status",
                                                "ACTIVE",
                                                "startDate",
                                                "2020-01-01T00:00:00",
                                                "endDate",
                                                "2020-01-31T00:00:00"),
                                        "products",
                                        List.of(comboItem(first, 1), comboItem(second, 1))))
                        .post("/combos")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("id");

        quote(List.of(Map.of("type", "COMBO", "comboId", combo, "quantity", 1)))
                .statusCode(400)
                .body("violations.field", hasItem("items[0]"));
    }

    @Test
    void rejectsAComboItemWithAdditionals() {
        quote(
                        List.of(
                                Map.of(
                                        "type",
                                        "COMBO",
                                        "comboId",
                                        1,
                                        "quantity",
                                        1,
                                        "additionals",
                                        List.of(Map.of("ingredientId", 1, "quantity", 1)))))
                .statusCode(400)
                .body("violations.field", hasItem("items[0]"));
    }

    @Test
    void returnsNotFoundForAnUnknownProductSize() {
        quote(List.of(productItem(999999, 1))).statusCode(404);
    }

    @Test
    void rejectsAnEmptyCart() {
        quote(List.of()).statusCode(400);
    }
}

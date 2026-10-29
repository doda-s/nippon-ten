package dev.nipponten;

import static dev.nipponten.TestAuth.asAdmin;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PromotionBusinessRulesTest {

    private static final String OCT_START = "2026-10-01T00:00:00";
    private static final String OCT_END = "2026-10-31T00:00:00";

    @Inject TestClock clock;

    private Map<String, Object> promotion(
            List<Long> productIds, List<Long> comboIds, long typeId, String start, String end) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Promo");
        body.put("description", "d");
        body.put("status", "ACTIVE");
        body.put("promotionTypeId", typeId);
        body.put("productIds", productIds);
        body.put("comboIds", comboIds);
        body.put("startDate", start);
        body.put("endDate", end);
        body.put("enablePromotionPoints", true);
        return body;
    }

    private Map<String, Object> promotion(long productId, long typeId, String start, String end) {
        return promotion(List.of(productId), List.of(), typeId, start, end);
    }

    private long create(Map<String, Object> body) {
        return asAdmin()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/promotions")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");
    }

    private void expectStatus(Map<String, Object> body, int status) {
        asAdmin()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/promotions")
                .then()
                .statusCode(status);
    }

    @Test
    void appliesToSeveralProductsAndCombos() {
        long size = TestFixtures.createSize("Grande-" + System.nanoTime());
        long type = TestFixtures.createPromotionType("desconto 10");
        long product =
                asAdmin()
                        .contentType(ContentType.JSON)
                        .body(
                                TestFixtures.productRegistration(
                                        "Pastel com preco",
                                        List.of(),
                                        List.of(TestFixtures.sizeWithPrice(size, 20.00))))
                        .post("/products")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("id");
        long other = TestFixtures.createProduct("Pastel varios");
        long combo = TestFixtures.createCombo("Combo promo");

        asAdmin()
                .contentType(ContentType.JSON)
                .body(promotion(List.of(product, other), List.of(combo), type, OCT_START, OCT_END))
                .post("/promotions")
                .then()
                .statusCode(201)
                .body("products.productId", containsInAnyOrder((int) product, (int) other))
                .body(
                        "products.find { it.productId == " + product + " }.prices[0].originalPrice",
                        comparesEqualTo(20.00f))
                .body(
                        "products.find { it.productId == "
                                + product
                                + " }.prices[0].promotionalPrice",
                        comparesEqualTo(18.00f))
                .body("combos[0].comboId", comparesEqualTo((int) combo))
                .body("combos[0].originalPrice", comparesEqualTo(30.00f))
                .body("combos[0].promotionalPrice", comparesEqualTo(27.00f));
    }

    @Test
    void requiresAtLeastOneItem() {
        long type = TestFixtures.createPromotionType("sem itens");

        expectStatus(promotion(List.of(), List.of(), type, OCT_START, OCT_END), 400);
    }

    @Test
    void rejectsDuplicatedItems() {
        long product = TestFixtures.createProduct("Pastel repetido");
        long type = TestFixtures.createPromotionType("repetido");

        expectStatus(
                promotion(List.of(product, product), List.of(), type, OCT_START, OCT_END), 400);
    }

    @Test
    void rejectsUnknownProductOrCombo() {
        long type = TestFixtures.createPromotionType("inexistente");

        expectStatus(promotion(List.of(999999L), List.of(), type, OCT_START, OCT_END), 404);
        expectStatus(promotion(List.of(), List.of(999999L), type, OCT_START, OCT_END), 404);
    }

    @Test
    void rejectsOverlappingPromotionsForTheSameItem() {
        long product = TestFixtures.createProduct("Pastel sobreposto");
        long combo = TestFixtures.createCombo("Combo sobreposto");
        long type = TestFixtures.createPromotionType("sobreposta");
        create(promotion(List.of(product), List.of(combo), type, OCT_START, OCT_END));

        expectStatus(promotion(product, type, "2026-10-15T00:00:00", "2026-11-15T00:00:00"), 400);
        expectStatus(
                promotion(
                        List.of(),
                        List.of(combo),
                        type,
                        "2026-09-15T00:00:00",
                        "2026-10-02T00:00:00"),
                400);
    }

    @Test
    void allowsSchedulingTheNextPromotionForTheSameItem() {
        long product = TestFixtures.createProduct("Pastel agendado");
        long type = TestFixtures.createPromotionType("agendada");
        create(promotion(product, type, OCT_START, OCT_END));

        expectStatus(promotion(product, type, OCT_END, "2026-11-30T00:00:00"), 201);
    }

    @Test
    void updateReplacesTheItemsAndDoesNotConflictWithItself() {
        long first = TestFixtures.createProduct("Pastel antes");
        long second = TestFixtures.createProduct("Pastel depois");
        long type = TestFixtures.createPromotionType("troca");
        long promotionId = create(promotion(first, type, OCT_START, OCT_END));

        asAdmin()
                .contentType(ContentType.JSON)
                .body(promotion(second, type, OCT_START, OCT_END))
                .put("/promotions/" + promotionId)
                .then()
                .statusCode(200)
                .body("products.productId", contains((int) second));

        // O primeiro produto saiu da promoção, então pode receber outra no mesmo período.
        expectStatus(promotion(first, type, OCT_START, OCT_END), 201);
    }

    @Test
    void blocksDeletingProductsAndCombosUsedByAPromotion() {
        long product = TestFixtures.createProduct("Pastel preso");
        long combo = TestFixtures.createCombo("Combo preso");
        long type = TestFixtures.createPromotionType("preso");
        long promotionId =
                create(promotion(List.of(product), List.of(combo), type, OCT_START, OCT_END));

        asAdmin().delete("/products/" + product).then().statusCode(400);
        asAdmin().delete("/combos/" + combo).then().statusCode(400);

        asAdmin()
                .get("/combos/" + combo + "/dependents")
                .then()
                .statusCode(200)
                .body("promotions.id", contains((int) promotionId));
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        long product = TestFixtures.createProduct("Pastel janela");
        long type = TestFixtures.createPromotionType("janela invalida");

        expectStatus(promotion(product, type, OCT_END, OCT_START), 400);
    }

    @Test
    void availableFilterHidesPromotionsOutsideTheWindow() {
        long product = TestFixtures.createProduct("Pastel expirado");
        long type = TestFixtures.createPromotionType("expirada");
        long promotionId =
                create(promotion(product, type, "2020-01-01T00:00:00", "2020-02-01T00:00:00"));

        asAdmin()
                .get("/promotions")
                .then()
                .statusCode(200)
                .body("content.id", hasItem((int) promotionId));

        asAdmin()
                .queryParam("available", true)
                .get("/promotions")
                .then()
                .statusCode(200)
                .body("content.id", not(hasItem((int) promotionId)));
    }

    @Test
    void availabilityUsesTheRestaurantTimeZone() {
        // Amanhã, 17h30 em São Paulo (20h30 UTC). A janela é 17h–18h no horário local: um servidor
        // que comparasse no próprio fuso (UTC) não a veria como vigente.
        ZoneId restaurant = ZoneId.of("America/Sao_Paulo");
        LocalDate tomorrow = LocalDate.now(restaurant).plusDays(1);
        Instant target = tomorrow.atTime(17, 30).atZone(restaurant).toInstant();
        long product = TestFixtures.createProduct("Pastel no fuso");
        long type = TestFixtures.createPromotionType("fuso");
        long promotionId =
                create(
                        promotion(
                                product,
                                type,
                                tomorrow.atTime(17, 0).toString(),
                                tomorrow.atTime(18, 0).toString()));

        try {
            clock.advance(Duration.between(Instant.now(), target));

            given().queryParam("available", true)
                    .get("/promotions")
                    .then()
                    .statusCode(200)
                    .body("content.id", hasItem((int) promotionId));
        } finally {
            clock.reset();
        }
    }
}

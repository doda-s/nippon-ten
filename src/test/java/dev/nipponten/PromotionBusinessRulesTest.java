package dev.nipponten;

import static dev.nipponten.TestAuth.asAdmin;
import static io.restassured.RestAssured.given;
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

    @Inject TestClock clock;

    private Map<String, Object> promotion(long productId, long typeId, String start, String end) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Promo");
        body.put("description", "d");
        body.put("status", "ACTIVE");
        body.put("promotionTypeId", typeId);
        body.put("productId", productId);
        body.put("startDate", start);
        body.put("endDate", end);
        body.put("enablePromotionPoints", true);
        return body;
    }

    @Test
    void allowsOnlyOnePromotionPerProduct() {
        long product = TestFixtures.createProduct("Pastel promo");
        long type = TestFixtures.createPromotionType("10 por cento");
        Map<String, Object> body =
                promotion(product, type, "2026-10-01T00:00:00", "2026-10-31T00:00:00");

        asAdmin()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/promotions")
                .then()
                .statusCode(201);

        asAdmin()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/promotions")
                .then()
                .statusCode(400);
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        long product = TestFixtures.createProduct("Pastel janela");
        long type = TestFixtures.createPromotionType("janela invalida");

        asAdmin()
                .contentType(ContentType.JSON)
                .body(promotion(product, type, "2026-10-31T00:00:00", "2026-10-01T00:00:00"))
                .post("/promotions")
                .then()
                .statusCode(400);
    }

    @Test
    void availableFilterHidesPromotionsOutsideTheWindow() {
        long product = TestFixtures.createProduct("Pastel expirado");
        long type = TestFixtures.createPromotionType("expirada");

        long promotionId =
                asAdmin()
                        .contentType(ContentType.JSON)
                        .body(
                                promotion(
                                        product,
                                        type,
                                        "2020-01-01T00:00:00",
                                        "2020-02-01T00:00:00"))
                        .post("/promotions")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("id");

        asAdmin().get("/promotions").then().statusCode(200).body("id", hasItem((int) promotionId));

        asAdmin()
                .queryParam("available", true)
                .get("/promotions")
                .then()
                .statusCode(200)
                .body("id", not(hasItem((int) promotionId)));
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
                asAdmin()
                        .contentType(ContentType.JSON)
                        .body(
                                promotion(
                                        product,
                                        type,
                                        tomorrow.atTime(17, 0).toString(),
                                        tomorrow.atTime(18, 0).toString()))
                        .post("/promotions")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("id");

        try {
            clock.advance(Duration.between(Instant.now(), target));

            given().queryParam("available", true)
                    .get("/promotions")
                    .then()
                    .statusCode(200)
                    .body("id", hasItem((int) promotionId));
        } finally {
            clock.reset();
        }
    }

    @Test
    void computesPromotionalPricePerProductSize() {
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

        asAdmin()
                .contentType(ContentType.JSON)
                .body(promotion(product, type, "2026-10-01T00:00:00", "2026-10-31T00:00:00"))
                .post("/promotions")
                .then()
                .statusCode(201)
                .body("prices[0].originalPrice", org.hamcrest.Matchers.comparesEqualTo(20.00f))
                .body("prices[0].promotionalPrice", org.hamcrest.Matchers.comparesEqualTo(18.00f));
    }
}

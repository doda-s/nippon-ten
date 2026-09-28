package dev.nipponten;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PromotionBusinessRulesTest {

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

        given().contentType(ContentType.JSON).body(body).post("/promotions").then().statusCode(201);

        given().contentType(ContentType.JSON).body(body).post("/promotions").then().statusCode(400);
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        long product = TestFixtures.createProduct("Pastel janela");
        long type = TestFixtures.createPromotionType("janela invalida");

        given().contentType(ContentType.JSON)
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
                given().contentType(ContentType.JSON)
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

        given().get("/promotions").then().statusCode(200).body("id", hasItem((int) promotionId));

        given().queryParam("available", true)
                .get("/promotions")
                .then()
                .statusCode(200)
                .body("id", not(hasItem((int) promotionId)));
    }

    @Test
    void computesPromotionalPricePerProductSize() {
        long product = TestFixtures.createProduct("Pastel com preco");
        long size = TestFixtures.createSize("Grande-" + System.nanoTime());
        long type = TestFixtures.createPromotionType("desconto 10");

        given().contentType(ContentType.JSON)
                .body(Map.of("sizeId", size, "price", 20.00, "status", "ACTIVE"))
                .post("/products/{id}/sizes", product)
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .body(promotion(product, type, "2026-10-01T00:00:00", "2026-10-31T00:00:00"))
                .post("/promotions")
                .then()
                .statusCode(201)
                .body("prices[0].originalPrice", org.hamcrest.Matchers.comparesEqualTo(20.00f))
                .body("prices[0].promotionalPrice", org.hamcrest.Matchers.comparesEqualTo(18.00f));
    }
}

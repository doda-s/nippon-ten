package dev.nipponten;

import static dev.nipponten.TestAuth.asAdmin;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ValidationErrorFormatTest {

    @Test
    void reportsBeanValidationFailuresInTheSameErrorShapeAsTheRestOfTheApi() {
        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "", "price", -1))
                .post("/ingredients")
                .then()
                .statusCode(400)
                .body("message", org.hamcrest.Matchers.is("Invalid request"))
                .body("violations", hasSize(3))
                .body("violations", everyItem(hasKey("field")))
                .body("violations", everyItem(hasKey("message")));
    }
}

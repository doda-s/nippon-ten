package dev.nipponten;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class UserRulesTest {

    private Map<String, Object> registration(String email) {
        return Map.of(
                "user", Map.of("email", email, "password", "secret123"),
                "client", Map.of("name", "Joao", "lastName", "Sumi"));
    }

    @Test
    void storesEmailNormalizedAndRejectsADifferentCasing() {
        String email = "Cliente." + System.nanoTime() + "@Example.COM";

        given().contentType(ContentType.JSON)
                .body(registration(email))
                .post("/users")
                .then()
                .statusCode(201)
                .body("email", is(email.toLowerCase()));

        given().contentType(ContentType.JSON)
                .body(registration(email.toUpperCase()))
                .post("/users")
                .then()
                .statusCode(400);
    }

    @Test
    void refusesToDeleteAnInternalUserThatIsStillActive() {
        long roleId =
                given().contentType(ContentType.JSON)
                        .body(
                                Map.of(
                                        "name",
                                        "gerente-" + System.nanoTime(),
                                        "permissions",
                                        List.of("MANAGE_USERS")))
                        .post("/internal-roles")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("id");

        long internalId =
                given().contentType(ContentType.JSON)
                        .body(
                                Map.of(
                                        "user",
                                        Map.of(
                                                "email",
                                                "interno." + System.nanoTime() + "@example.com",
                                                "password",
                                                "secret123"),
                                        "internal",
                                        Map.of(
                                                "internalRoleId",
                                                roleId,
                                                "userPermissions",
                                                List.of("MANAGE_PRODUCTS"),
                                                "name",
                                                "Interno",
                                                "lastName",
                                                "Teste",
                                                "cpf",
                                                "12345678901")))
                        .post("/internal")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("id");

        given().get("/internal/{id}", internalId)
                .then()
                .statusCode(200)
                .body("userPermissions", is(List.of("MANAGE_PRODUCTS")));

        given().delete("/internal/{id}", internalId).then().statusCode(400);
    }

    @Test
    void internalRoleIsCreatedActiveAndCanBeToggled() {
        long roleId =
                given().contentType(ContentType.JSON)
                        .body(
                                Map.of(
                                        "name",
                                        "caixa-" + System.nanoTime(),
                                        "permissions",
                                        List.of("MANAGE_USERS")))
                        .post("/internal-roles")
                        .then()
                        .statusCode(201)
                        .body("active", is(true))
                        .extract()
                        .jsonPath()
                        .getLong("id");

        given().contentType(ContentType.JSON)
                .post("/internal-roles/{id}/deactivate", roleId)
                .then()
                .statusCode(200)
                .body("active", is(false));

        given().contentType(ContentType.JSON)
                .body(Map.of("name", "caixa-" + System.nanoTime(), "permissions", List.of()))
                .put("/internal-roles/{id}", roleId)
                .then()
                .statusCode(200)
                .body("active", is(false));

        given().contentType(ContentType.JSON)
                .post("/internal-roles/{id}/activate", roleId)
                .then()
                .statusCode(200)
                .body("active", is(true));
    }
}

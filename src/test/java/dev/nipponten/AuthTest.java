package dev.nipponten;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AuthTest {

    private static final String PASSWORD = "secret123";

    private static String uniqueEmail(String prefix) {
        return prefix + "." + System.nanoTime() + "@example.com";
    }

    private static long registerClient(String email) {
        return given().contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "user", Map.of("email", email, "password", PASSWORD),
                                "client", Map.of("name", "Joao", "lastName", "Sumi")))
                .post("/users")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");
    }

    private static long createRole(List<String> permissions) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("name", "role-" + System.nanoTime(), "permissions", permissions))
                .post("/internal-roles")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");
    }

    private static long registerInternal(String email, long roleId, List<String> userPermissions) {
        return given().contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "user", Map.of("email", email, "password", PASSWORD),
                                "internal",
                                        Map.of(
                                                "internalRoleId", roleId,
                                                "userPermissions", userPermissions,
                                                "name", "Interno",
                                                "lastName", "Teste",
                                                "cpf", "12345678901")))
                .post("/internal")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("userId");
    }

    private static String login(String email) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", PASSWORD))
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("tokenType", is("Bearer"))
                .body("expiresIn", is(900))
                .extract()
                .jsonPath()
                .getString("accessToken");
    }

    @Test
    void clientLogsInAndSeesItsOwnProfile() {
        String email = uniqueEmail("cliente");
        long userId = registerClient(email);

        given().auth()
                .oauth2(login(email))
                .get("/auth/me")
                .then()
                .statusCode(200)
                .body("userId", is((int) userId))
                .body("email", is(email))
                .body("type", is("CLIENT"))
                .body("name", is("Joao"))
                .body("permissions", is(List.of()));
    }

    @Test
    void loginIgnoresEmailCasing() {
        String email = uniqueEmail("cliente");
        registerClient(email);

        given().contentType(ContentType.JSON)
                .body(Map.of("email", email.toUpperCase(), "password", PASSWORD))
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue());
    }

    @Test
    void wrongPasswordUnknownEmailAndInactiveUserGetTheSame401() {
        String email = uniqueEmail("cliente");
        long userId = registerClient(email);
        String inactiveEmail = uniqueEmail("inativo");
        long inactiveId = registerClient(inactiveEmail);
        given().contentType(ContentType.JSON)
                .post("/users/{id}/deactivate", inactiveId)
                .then()
                .statusCode(200);

        for (Map<String, String> credentials :
                List.of(
                        Map.of("email", email, "password", "wrong-password"),
                        Map.of("email", uniqueEmail("ninguem"), "password", PASSWORD),
                        Map.of("email", inactiveEmail, "password", PASSWORD))) {
            given().contentType(ContentType.JSON)
                    .body(credentials)
                    .post("/auth/login")
                    .then()
                    .statusCode(401)
                    .body("message", is("Invalid email or password"));
        }
        given().get("/users/{id}", userId).then().statusCode(200);
    }

    @Test
    void meRequiresAValidToken() {
        given().get("/auth/me")
                .then()
                .statusCode(401)
                .contentType(ContentType.JSON)
                .body("message", is("Invalid or expired token"));
        given().auth()
                .oauth2("not-a-jwt")
                .get("/auth/me")
                .then()
                .statusCode(401)
                .contentType(ContentType.JSON)
                .body("message", is("Invalid or expired token"));
    }

    @Test
    void tokenStopsWorkingAsSoonAsTheUserIsDeactivated() {
        String email = uniqueEmail("cliente");
        long userId = registerClient(email);
        String token = login(email);

        given().auth().oauth2(token).get("/auth/me").then().statusCode(200);
        given().contentType(ContentType.JSON)
                .post("/users/{id}/deactivate", userId)
                .then()
                .statusCode(200);
        given().auth()
                .oauth2(token)
                .get("/auth/me")
                .then()
                .statusCode(401)
                .contentType(ContentType.JSON)
                .body("message", is("Invalid or expired token"));
    }

    @Test
    void internalGetsRolePlusIndividualPermissionsAndLosesOnlyTheRoleOnesWhenItIsDeactivated() {
        long roleId = createRole(List.of("MANAGE_PRODUCTS", "MANAGE_INGREDIENTS"));
        String email = uniqueEmail("interno");
        registerInternal(email, roleId, List.of("MANAGE_USERS"));
        String token = login(email);

        given().auth()
                .oauth2(token)
                .get("/auth/me")
                .then()
                .statusCode(200)
                .body("type", is("INTERNAL"))
                .body("name", is("Interno"))
                .body(
                        "permissions",
                        is(List.of("MANAGE_USERS", "MANAGE_PRODUCTS", "MANAGE_INGREDIENTS")));

        given().contentType(ContentType.JSON)
                .post("/internal-roles/{id}/deactivate", roleId)
                .then()
                .statusCode(200);

        given().auth()
                .oauth2(token)
                .get("/auth/me")
                .then()
                .statusCode(200)
                .body("permissions", is(List.of("MANAGE_USERS")));
    }

    @Test
    void changingAnInternalsPermissionsAppliesToTheNextRequest() {
        long roleId = createRole(List.of("MANAGE_PRODUCTS"));
        String email = uniqueEmail("interno");
        long userId = registerInternal(email, roleId, List.of());
        long internalId =
                given().get("/internal")
                        .then()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getLong("find { it.userId == " + userId + " }.id");
        String token = login(email);
        given().auth()
                .oauth2(token)
                .get("/auth/me")
                .then()
                .body("permissions", is(List.of("MANAGE_PRODUCTS")));

        given().contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "internalRoleId", roleId,
                                "userPermissions", List.of("MANAGE_USERS"),
                                "name", "Interno",
                                "lastName", "Teste",
                                "cpf", "12345678901"))
                .put("/internal/{id}", internalId)
                .then()
                .statusCode(200);

        given().auth()
                .oauth2(token)
                .get("/auth/me")
                .then()
                .statusCode(200)
                .body("permissions", is(List.of("MANAGE_USERS", "MANAGE_PRODUCTS")));
    }
}

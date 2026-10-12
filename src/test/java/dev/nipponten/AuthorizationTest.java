package dev.nipponten;

import static dev.nipponten.TestAuth.as;
import static dev.nipponten.TestAuth.asAdmin;
import static dev.nipponten.TestAuth.login;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.nipponten.domain.models.InternalPermission;
import dev.nipponten.infrastructure.security.PermissionIdentitySupplier;
import dev.nipponten.resource.security.Permissions;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AuthorizationTest {

    private static final String PASSWORD = "secret123";

    /** Rota protegida e a permissão que libera. */
    private record Route(String method, String path, String permission) {}

    // Um caso por linha da matriz; ids inexistentes bastam, porque a checagem vem antes do Service.
    private static final List<Route> PROTECTED =
            List.of(
                    new Route("POST", "/products", Permissions.MANAGE_PRODUCTS),
                    new Route("PUT", "/products/999999", Permissions.MANAGE_PRODUCTS),
                    new Route("DELETE", "/products/999999", Permissions.MANAGE_PRODUCTS),
                    new Route("POST", "/products/999999/sizes", Permissions.MANAGE_PRODUCTS),
                    new Route("GET", "/products/999999/dependents", Permissions.MANAGE_PRODUCTS),
                    new Route("POST", "/sizes", Permissions.MANAGE_PRODUCTS),
                    new Route("POST", "/ingredients", Permissions.MANAGE_INGREDIENTS),
                    new Route(
                            "GET",
                            "/ingredients/999999/dependents",
                            Permissions.MANAGE_INGREDIENTS),
                    new Route("POST", "/combos", Permissions.MANAGE_PROMOTIONS),
                    new Route("POST", "/promotions", Permissions.MANAGE_PROMOTIONS),
                    new Route("POST", "/promotion-types", Permissions.MANAGE_PROMOTIONS),
                    new Route("GET", "/internal-roles", Permissions.MANAGE_ROLES),
                    new Route("POST", "/internal-roles", Permissions.MANAGE_ROLES),
                    new Route("GET", "/internal", Permissions.MANAGE_USERS),
                    new Route("POST", "/internal", Permissions.MANAGE_USERS),
                    new Route("GET", "/users", Permissions.MANAGE_USERS),
                    new Route("POST", "/users/999999/activate", Permissions.MANAGE_USERS));

    private static final List<String> PUBLIC_READS =
            List.of(
                    "/products",
                    "/sizes",
                    "/ingredients",
                    "/combos",
                    "/promotions",
                    "/promotion-types");

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

    private static String clientToken() {
        String email = uniqueEmail("cliente");
        registerClient(email);
        return login(email, PASSWORD);
    }

    private static String internalToken(List<String> permissions) {
        long roleId =
                asAdmin()
                        .contentType(ContentType.JSON)
                        .body(
                                Map.of(
                                        "name",
                                        "role-" + System.nanoTime(),
                                        "permissions",
                                        permissions))
                        .post("/internal-roles")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("id");
        String email = uniqueEmail("interno");
        asAdmin()
                .contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "user", Map.of("email", email, "password", PASSWORD),
                                "internal",
                                        Map.of(
                                                "internalRoleId", roleId,
                                                "userPermissions", List.of(),
                                                "name", "Interno",
                                                "lastName", "Teste",
                                                "cpf", "12345678901")))
                .post("/internal")
                .then()
                .statusCode(201);
        return login(email, PASSWORD);
    }

    private static int call(RequestSpecification spec, Route route) {
        return spec.contentType(ContentType.JSON)
                .body("{}")
                .request(route.method(), route.path())
                .statusCode();
    }

    @Test
    void permissionConstantsMatchTheEnum() throws ReflectiveOperationException {
        Set<String> constants =
                Arrays.stream(Permissions.class.getDeclaredFields())
                        .filter(field -> Modifier.isStatic(field.getModifiers()))
                        .map(Field::getName)
                        .filter(name -> !name.equals("SUPER_ADMIN"))
                        .collect(Collectors.toSet());
        Set<String> permissions =
                Arrays.stream(InternalPermission.values())
                        .map(Enum::name)
                        .collect(Collectors.toSet());
        assertEquals(permissions, constants);
        for (String name : constants) {
            assertEquals(name, Permissions.class.getField(name).get(null));
        }
        assertEquals(PermissionIdentitySupplier.SUPER_ADMIN_ROLE, Permissions.SUPER_ADMIN);
    }

    @Test
    void protectedRoutesAnswer401WithoutTokenAnd403WithoutThePermission() {
        String client = clientToken();
        String noPermissions = internalToken(List.of());
        for (Route route : PROTECTED) {
            assertEquals(401, call(given(), route), "anônimo em " + route);
            assertEquals(403, call(as(client), route), "cliente em " + route);
            assertEquals(403, call(as(noPermissions), route), "interno sem permissão em " + route);
        }
    }

    @Test
    void eachPermissionOpensItsRoutes() {
        Map<String, String> tokens =
                Arrays.stream(InternalPermission.values())
                        .map(Enum::name)
                        .collect(
                                Collectors.toMap(
                                        permission -> permission,
                                        permission -> internalToken(List.of(permission))));
        for (Route route : PROTECTED) {
            int status = call(as(tokens.get(route.permission())), route);
            assertFalse(status == 401 || status == 403, route + " devolveu " + status);
        }
    }

    @Test
    void theMenuIsPublicForReading() {
        for (String path : PUBLIC_READS) {
            given().get(path).then().statusCode(200);
        }
    }

    @Test
    void onlyASuperAdminDeletesInternals() {
        as(internalToken(List.of(Permissions.MANAGE_USERS)))
                .delete("/internal/{id}", 999999)
                .then()
                .statusCode(403);
        asAdmin().delete("/internal/{id}", 999999).then().statusCode(404);
    }

    @Test
    void aClientManagesItsOwnAccountOnly() {
        String email = uniqueEmail("dono");
        long self = registerClient(email);
        String token = login(email, PASSWORD);
        long other = registerClient(uniqueEmail("outro"));

        as(token).get("/users/{id}", self).then().statusCode(200);
        as(token).get("/users/{id}/client", self).then().statusCode(200);
        as(token).get("/users/{id}/addresses", self).then().statusCode(200);
        as(token)
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", PASSWORD))
                .put("/users/{id}", self)
                .then()
                .statusCode(200);

        as(token).get("/users/{id}", other).then().statusCode(403);
        as(token).get("/users/{id}/client", other).then().statusCode(403);
        as(token).get("/users/{id}/addresses", other).then().statusCode(403);
        as(token)
                .contentType(ContentType.JSON)
                .body(Map.of("email", uniqueEmail("x"), "password", PASSWORD))
                .put("/users/{id}", other)
                .then()
                .statusCode(403);
        as(token)
                .contentType(ContentType.JSON)
                .post("/users/{id}/deactivate", other)
                .then()
                .statusCode(403);
        as(token).delete("/users/{id}", other).then().statusCode(403);
        // Um id que não existe também dá 403, sem revelar se o usuário existe.
        as(token).get("/users/{id}", 999999).then().statusCode(403);
        asAdmin().get("/users/{id}", other).then().statusCode(200);
    }

    @Test
    void aClientCanDeactivateButNotReactivateItself() {
        String email = uniqueEmail("dono");
        long self = registerClient(email);
        String token = login(email, PASSWORD);

        as(token)
                .contentType(ContentType.JSON)
                .post("/users/{id}/activate", self)
                .then()
                .statusCode(403);
        as(token)
                .contentType(ContentType.JSON)
                .post("/users/{id}/deactivate", self)
                .then()
                .statusCode(200);
        as(token).get("/auth/me").then().statusCode(401);
    }

    @Test
    void aClientCanDeleteItsOwnAccount() {
        String email = uniqueEmail("dono");
        long self = registerClient(email);

        as(login(email, PASSWORD)).delete("/users/{id}", self).then().statusCode(204);
        asAdmin().get("/users/{id}", self).then().statusCode(404);
    }

    @Test
    void aUserManagerReachesOtherUsers() {
        long other = registerClient(uniqueEmail("outro"));

        as(internalToken(List.of(Permissions.MANAGE_USERS)))
                .get("/users/{id}", other)
                .then()
                .statusCode(200);
    }

    @Test
    void openApiDeclaresTheBearerSchemeAndStaysPublic() {
        given().get("/q/openapi").then().statusCode(200).body(containsString("bearer"));
        given().get("/api/index.html").then().statusCode(oneOf(200, 302, 301));
    }
}

package dev.nipponten;

import static dev.nipponten.TestAuth.as;
import static dev.nipponten.TestAuth.asAdmin;
import static dev.nipponten.TestAuth.login;
import static org.hamcrest.Matchers.is;

import dev.nipponten.domain.models.Internal;
import dev.nipponten.domain.repositories.InternalRepository;
import dev.nipponten.domain.repositories.InternalRoleRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PrivilegeEscalationTest {

    private static final String PASSWORD = "secret123";

    @Inject InternalRepository internalRepository;

    @Inject InternalRoleRepository roleRepository;

    /** Interno criado pelo admin, com a role e as permissões individuais pedidas. */
    private record Staff(long internalId, long userId, long roleId, String email) {
        String token() {
            return login(email, PASSWORD);
        }
    }

    private static long createRole(List<String> permissions) {
        return asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "role-" + System.nanoTime(), "permissions", permissions))
                .post("/internal-roles")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");
    }

    private static Map<String, Object> internalBody(long roleId, List<String> userPermissions) {
        return Map.of(
                "internalRoleId", roleId,
                "userPermissions", userPermissions,
                "name", "Interno",
                "lastName", "Teste",
                "cpf", "12345678901");
    }

    private static Map<String, Object> registration(
            String email, long roleId, List<String> userPermissions) {
        return Map.of(
                "user", Map.of("email", email, "password", PASSWORD),
                "internal", internalBody(roleId, userPermissions));
    }

    private static String uniqueEmail() {
        return "interno." + System.nanoTime() + "@example.com";
    }

    private static Staff staff(List<String> rolePermissions, List<String> userPermissions) {
        long roleId = createRole(rolePermissions);
        String email = uniqueEmail();
        JsonPath created =
                asAdmin()
                        .contentType(ContentType.JSON)
                        .body(registration(email, roleId, userPermissions))
                        .post("/internal")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath();
        return new Staff(created.getLong("id"), created.getLong("userId"), roleId, email);
    }

    private static Staff userManager() {
        return staff(List.of("MANAGE_USERS"), List.of());
    }

    private static long adminUserId() {
        return asAdmin().get("/auth/me").then().extract().jsonPath().getLong("userId");
    }

    @Test
    void aManagerCannotGrantIndividualPermissionsItDoesNotHave() {
        String manager = userManager().token();
        long roleId = createRole(List.of());

        as(manager)
                .contentType(ContentType.JSON)
                .body(registration(uniqueEmail(), roleId, List.of("MANAGE_PRODUCTS")))
                .post("/internal")
                .then()
                .statusCode(403)
                .body("message", is("Cannot grant permissions you do not have: [MANAGE_PRODUCTS]"));
        as(manager)
                .contentType(ContentType.JSON)
                .body(registration(uniqueEmail(), roleId, List.of("MANAGE_USERS")))
                .post("/internal")
                .then()
                .statusCode(201);
    }

    @Test
    void aManagerCannotAssignARoleWithPermissionsItDoesNotHave() {
        String manager = userManager().token();
        long productsRole = createRole(List.of("MANAGE_PRODUCTS"));
        long usersRole = createRole(List.of("MANAGE_USERS"));
        Staff target = staff(List.of(), List.of());

        as(manager)
                .contentType(ContentType.JSON)
                .body(registration(uniqueEmail(), productsRole, List.of()))
                .post("/internal")
                .then()
                .statusCode(403);
        as(manager)
                .contentType(ContentType.JSON)
                .body(internalBody(productsRole, List.of()))
                .put("/internal/{id}", target.internalId())
                .then()
                .statusCode(403);
        as(manager)
                .contentType(ContentType.JSON)
                .body(internalBody(usersRole, List.of()))
                .put("/internal/{id}", target.internalId())
                .then()
                .statusCode(200);
    }

    @Test
    void editingAnInternalOnlyChecksWhatChanges() {
        String manager = userManager().token();
        // O alvo já tem uma role e uma permissão individual que o gerente não tem.
        Staff target = staff(List.of("MANAGE_PRODUCTS"), List.of("MANAGE_PROMOTIONS"));
        Map<String, Object> renamed =
                new HashMap<>(internalBody(target.roleId(), List.of("MANAGE_PROMOTIONS")));
        renamed.put("name", "Renomeado");

        as(manager)
                .contentType(ContentType.JSON)
                .body(renamed)
                .put("/internal/{id}", target.internalId())
                .then()
                .statusCode(200)
                .body("name", is("Renomeado"));
    }

    @Test
    void nobodyChangesTheirOwnRoleOrPermissions() {
        Staff self = staff(List.of("MANAGE_USERS"), List.of());
        String token = self.token();
        long otherRole = createRole(List.of("MANAGE_USERS"));

        as(token)
                .contentType(ContentType.JSON)
                .body(internalBody(self.roleId(), List.of("MANAGE_USERS")))
                .put("/internal/{id}", self.internalId())
                .then()
                .statusCode(403)
                .body("message", is("Cannot change your own role or permissions"));
        as(token)
                .contentType(ContentType.JSON)
                .body(internalBody(otherRole, List.of()))
                .put("/internal/{id}", self.internalId())
                .then()
                .statusCode(403);
        Map<String, Object> renamed = new HashMap<>(internalBody(self.roleId(), List.of()));
        renamed.put("name", "Proprio");
        as(token)
                .contentType(ContentType.JSON)
                .body(renamed)
                .put("/internal/{id}", self.internalId())
                .then()
                .statusCode(200);

        // Vale também para o super_admin.
        Internal admin = internalRepository.getByUser(adminUserId()).getFirst();
        asAdmin()
                .contentType(ContentType.JSON)
                .body(internalBody(admin.internalRoleId(), List.of("MANAGE_USERS")))
                .put("/internal/{id}", admin.id())
                .then()
                .statusCode(403);
    }

    @Test
    void aRoleManagerCannotCreateEditOrReactivateRolesBeyondItsPermissions() {
        String manager = staff(List.of("MANAGE_ROLES"), List.of()).token();
        long productsRole = createRole(List.of("MANAGE_PRODUCTS"));

        as(manager)
                .contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "name",
                                "role-" + System.nanoTime(),
                                "permissions",
                                List.of("MANAGE_PRODUCTS")))
                .post("/internal-roles")
                .then()
                .statusCode(403);
        long own =
                as(manager)
                        .contentType(ContentType.JSON)
                        .body(
                                Map.of(
                                        "name",
                                        "role-" + System.nanoTime(),
                                        "permissions",
                                        List.of("MANAGE_ROLES")))
                        .post("/internal-roles")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("id");
        as(manager)
                .contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "name",
                                "role-" + System.nanoTime(),
                                "permissions",
                                List.of("MANAGE_ROLES", "MANAGE_USERS")))
                .put("/internal-roles/{id}", own)
                .then()
                .statusCode(403);

        asAdmin()
                .contentType(ContentType.JSON)
                .post("/internal-roles/{id}/deactivate", productsRole)
                .then()
                .statusCode(200);
        as(manager)
                .contentType(ContentType.JSON)
                .post("/internal-roles/{id}/activate", productsRole)
                .then()
                .statusCode(403);
        as(manager)
                .contentType(ContentType.JSON)
                .post("/internal-roles/{id}/deactivate", own)
                .then()
                .statusCode(200);
    }

    @Test
    void aUserManagerCannotEditOrDeactivateASuperAdmin() {
        String manager = userManager().token();
        long admin = adminUserId();

        as(manager).get("/users/{id}", admin).then().statusCode(200);
        as(manager)
                .contentType(ContentType.JSON)
                .body(Map.of("email", "invasor@example.com", "password", "nova-senha"))
                .put("/users/{id}", admin)
                .then()
                .statusCode(403);
        as(manager)
                .contentType(ContentType.JSON)
                .post("/users/{id}/deactivate", admin)
                .then()
                .statusCode(403);
        as(manager).delete("/users/{id}", admin).then().statusCode(403);
        asAdmin().get("/auth/me").then().statusCode(200);
    }

    @Test
    void theLastActiveSuperAdminCannotBeDeactivated() {
        long admin = adminUserId();
        long reservedRole = roleRepository.getReserved().id();
        // Outros testes podem ter criado super_admins; desativá-los é permitido enquanto sobra um.
        for (Internal other : internalRepository.getByInternalRole(reservedRole)) {
            if (!other.userId().equals(admin)) {
                asAdmin()
                        .contentType(ContentType.JSON)
                        .post("/users/{id}/deactivate", other.userId())
                        .then()
                        .statusCode(200);
            }
        }

        asAdmin()
                .contentType(ContentType.JSON)
                .post("/users/{id}/deactivate", admin)
                .then()
                .statusCode(400);
        asAdmin().get("/auth/me").then().statusCode(200);
    }

    @Test
    void deletingAnInternalAccountThroughUsersFollowsTheInternalRules() {
        Staff target = staff(List.of(), List.of());

        as(userManager().token()).delete("/users/{id}", target.userId()).then().statusCode(403);
        asAdmin().delete("/users/{id}", target.userId()).then().statusCode(400);
        asAdmin()
                .contentType(ContentType.JSON)
                .post("/users/{id}/deactivate", target.userId())
                .then()
                .statusCode(200);
        asAdmin().delete("/users/{id}", target.userId()).then().statusCode(204);
        asAdmin().get("/internal/{id}", target.internalId()).then().statusCode(404);
    }
}

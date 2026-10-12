package dev.nipponten;

import static dev.nipponten.TestAuth.asAdmin;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.nipponten.application.services.SuperAdminService;
import dev.nipponten.domain.models.InternalPermission;
import dev.nipponten.domain.models.InternalRole;
import dev.nipponten.domain.repositories.InternalRepository;
import dev.nipponten.domain.repositories.InternalRoleRepository;
import dev.nipponten.domain.repositories.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SuperAdminTest {

    // Definidos com %test em application.properties.
    private static final String ADMIN_EMAIL = "admin@nippon-ten.test";
    private static final String ADMIN_PASSWORD = "test-admin-password";
    private static final String PASSWORD = "secret123";

    private static final List<String> ALL_PERMISSIONS =
            Arrays.stream(InternalPermission.values()).map(Enum::name).toList();

    @Inject SuperAdminService superAdminService;

    @Inject InternalRoleRepository roleRepository;

    @Inject InternalRepository internalRepository;

    @Inject UserRepository userRepository;

    private static String login(String email, String password) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("accessToken");
    }

    private static long reservedRoleId() {
        return asAdmin()
                .get("/internal-roles")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getLong("find { it.reserved == true }.id");
    }

    private static Map<String, Object> internalRegistration(String email, long roleId) {
        return Map.of(
                "user", Map.of("email", email, "password", PASSWORD),
                "internal",
                        Map.of(
                                "internalRoleId", roleId,
                                "userPermissions", List.of(),
                                "name", "Interno",
                                "lastName", "Teste",
                                "cpf", "12345678901"));
    }

    private static String internalWithoutSuperAdmin() {
        long roleId =
                asAdmin()
                        .contentType(ContentType.JSON)
                        .body(
                                Map.of(
                                        "name",
                                        "role-" + System.nanoTime(),
                                        "permissions",
                                        List.of("MANAGE_USERS")))
                        .post("/internal-roles")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("id");
        String email = "gerente." + System.nanoTime() + "@example.com";
        asAdmin()
                .contentType(ContentType.JSON)
                .body(internalRegistration(email, roleId))
                .post("/internal")
                .then()
                .statusCode(201);
        return login(email, PASSWORD);
    }

    @Test
    void startupCreatesTheReservedRoleAndTheFirstSuperAdmin() {
        asAdmin()
                .get("/internal-roles/{id}", reservedRoleId())
                .then()
                .statusCode(200)
                .body("name", is(SuperAdminService.ROLE_NAME))
                .body("active", is(true))
                .body("defaultRole", is(false));

        given().auth()
                .oauth2(login(ADMIN_EMAIL, ADMIN_PASSWORD))
                .get("/auth/me")
                .then()
                .statusCode(200)
                .body("type", is("INTERNAL"))
                .body("superAdmin", is(true))
                .body("permissions", is(ALL_PERMISSIONS));
    }

    @Test
    void bootstrapIsIdempotentAndBringsThePermissionsBackInSyncWithTheEnum() {
        InternalRole reserved = roleRepository.getReserved();
        roleRepository.save(
                new InternalRole(
                        reserved.id(),
                        reserved.name(),
                        List.of(InternalPermission.MANAGE_USERS),
                        false,
                        true,
                        true));
        String otherEmail = "outro.admin." + System.nanoTime() + "@example.com";
        // Outros testes criam super_admins; o que importa é o bootstrap não criar mais nenhum.
        int superAdmins = internalRepository.getByInternalRole(reserved.id()).size();

        superAdminService.bootstrap(Optional.of(otherEmail), Optional.of(PASSWORD));
        superAdminService.bootstrap(Optional.of(otherEmail), Optional.of(PASSWORD));

        assertEquals(
                Set.of(InternalPermission.values()),
                Set.copyOf(roleRepository.getReserved().permissions()));
        assertEquals(superAdmins, internalRepository.getByInternalRole(reserved.id()).size());
        // Já existe um super_admin, então as variáveis não criam outro.
        assertNull(userRepository.getByEmail(otherEmail));
    }

    @Test
    void reservedRoleCannotBeEditedDeactivatedOrDeleted() {
        long roleId = reservedRoleId();

        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "renomeada", "permissions", List.of(), "defaultRole", true))
                .put("/internal-roles/{id}", roleId)
                .then()
                .statusCode(400);
        asAdmin()
                .contentType(ContentType.JSON)
                .post("/internal-roles/{id}/deactivate", roleId)
                .then()
                .statusCode(400);
        asAdmin().delete("/internal-roles/{id}", roleId).then().statusCode(400);

        asAdmin()
                .get("/internal-roles/{id}", roleId)
                .then()
                .body("name", is(SuperAdminService.ROLE_NAME))
                .body("active", is(true))
                .body("defaultRole", is(false));
    }

    @Test
    void onlyASuperAdminAssignsTheReservedRole() {
        long roleId = reservedRoleId();

        given().contentType(ContentType.JSON)
                .body(
                        internalRegistration(
                                "sem.token." + System.nanoTime() + "@example.com", roleId))
                .post("/internal")
                .then()
                .statusCode(401);
        given().auth()
                .oauth2(internalWithoutSuperAdmin())
                .contentType(ContentType.JSON)
                .body(internalRegistration("gerente." + System.nanoTime() + "@example.com", roleId))
                .post("/internal")
                .then()
                .statusCode(403);
        given().auth()
                .oauth2(login(ADMIN_EMAIL, ADMIN_PASSWORD))
                .contentType(ContentType.JSON)
                .body(
                        internalRegistration(
                                "novo.admin." + System.nanoTime() + "@example.com", roleId))
                .post("/internal")
                .then()
                .statusCode(201);
    }

    @Test
    void onlyASuperAdminEditsAnInternalThatHasTheReservedRole() {
        long roleId = reservedRoleId();
        long adminInternalId = internalRepository.getByInternalRole(roleId).getFirst().id();
        Map<String, Object> body =
                Map.of(
                        "internalRoleId", roleId,
                        "userPermissions", List.of(),
                        "name", "Super",
                        "lastName", "Admin",
                        "cpf", "12345678901");

        given().auth()
                .oauth2(internalWithoutSuperAdmin())
                .contentType(ContentType.JSON)
                .body(body)
                .put("/internal/{id}", adminInternalId)
                .then()
                .statusCode(403);
        given().auth()
                .oauth2(login(ADMIN_EMAIL, ADMIN_PASSWORD))
                .contentType(ContentType.JSON)
                .body(body)
                .put("/internal/{id}", adminInternalId)
                .then()
                .statusCode(200);
    }
}

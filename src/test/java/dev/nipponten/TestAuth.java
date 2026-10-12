package dev.nipponten;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Login real como o super_admin criado pelo bootstrap (credenciais em %test, no
 * application.properties). O token é reaproveitado entre testes e renovado antes de expirar.
 */
final class TestAuth {

    static final String ADMIN_EMAIL = "admin@nippon-ten.test";
    static final String ADMIN_PASSWORD = "test-admin-password";

    // Bem abaixo dos 15 minutos do access token.
    private static final Duration REUSE_FOR = Duration.ofMinutes(10);

    private static String adminToken;
    private static Instant adminTokenUntil = Instant.MIN;

    private TestAuth() {}

    static synchronized String adminToken() {
        if (adminToken == null || Instant.now().isAfter(adminTokenUntil)) {
            adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
            adminTokenUntil = Instant.now().plus(REUSE_FOR);
        }
        return adminToken;
    }

    /** Request autenticado como super_admin, que tem todas as permissões. */
    static RequestSpecification asAdmin() {
        return given().auth().oauth2(adminToken());
    }

    static RequestSpecification as(String token) {
        return given().auth().oauth2(token);
    }

    static String login(String email, String password) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("accessToken");
    }
}

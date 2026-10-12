package dev.nipponten;

import static dev.nipponten.TestAuth.asAdmin;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RefreshTokenTest {

    private static final String PASSWORD = "secret123";
    private static final String COOKIE = "nt_refresh";
    private static final String CSRF_HEADER = "X-Requested-With";
    private static final String CSRF_VALUE = "nippon-ten";

    private static String uniqueEmail() {
        return "sessao." + System.nanoTime() + "@example.com";
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

    private static Response login(String email) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", PASSWORD))
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .response();
    }

    private static Response refresh(String refreshToken) {
        return given().contentType(ContentType.JSON)
                .header(CSRF_HEADER, CSRF_VALUE)
                .cookie(COOKIE, refreshToken)
                .post("/auth/refresh");
    }

    @Test
    void loginSetsAHardenedRefreshCookie() {
        String email = uniqueEmail();
        registerClient(email);

        given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", PASSWORD))
                .post("/auth/login")
                .then()
                .statusCode(200)
                .cookie(COOKIE, notNullValue())
                .header(
                        "Set-Cookie",
                        allOf(
                                containsString("HttpOnly"),
                                containsString("Secure"),
                                containsString("SameSite=None"),
                                containsString("Path=/auth"),
                                containsString("Max-Age=604800")));
    }

    @Test
    void refreshRotatesTheTokenAndIssuesAWorkingAccessToken() {
        String email = uniqueEmail();
        registerClient(email);
        String first = login(email).cookie(COOKIE);

        Response refreshed = refresh(first).then().statusCode(200).extract().response();
        String second = refreshed.cookie(COOKIE);
        assertNotEquals(first, second);

        given().auth()
                .oauth2(refreshed.jsonPath().getString("accessToken"))
                .get("/auth/me")
                .then()
                .statusCode(200);
        refresh(second).then().statusCode(200);
    }

    @Test
    void reusingARotatedTokenRevokesTheWholeChain() {
        String email = uniqueEmail();
        registerClient(email);
        String first = login(email).cookie(COOKIE);
        String second = refresh(first).then().statusCode(200).extract().cookie(COOKIE);

        refresh(first).then().statusCode(401);
        refresh(second).then().statusCode(401);
    }

    @Test
    void refreshAndLogoutRequireTheCsrfHeader() {
        String email = uniqueEmail();
        registerClient(email);
        String token = login(email).cookie(COOKIE);

        given().contentType(ContentType.JSON)
                .cookie(COOKIE, token)
                .post("/auth/refresh")
                .then()
                .statusCode(403);
        given().contentType(ContentType.JSON)
                .cookie(COOKIE, token)
                .post("/auth/logout")
                .then()
                .statusCode(403);
        refresh(token).then().statusCode(200);
    }

    @Test
    void refreshWithoutOrWithAnUnknownCookieIs401() {
        given().contentType(ContentType.JSON)
                .header(CSRF_HEADER, CSRF_VALUE)
                .post("/auth/refresh")
                .then()
                .statusCode(401);
        refresh("unknown-token").then().statusCode(401);
    }

    @Test
    void logoutRevokesTheTokenAndClearsTheCookie() {
        String email = uniqueEmail();
        registerClient(email);
        String token = login(email).cookie(COOKIE);

        given().contentType(ContentType.JSON)
                .header(CSRF_HEADER, CSRF_VALUE)
                .cookie(COOKIE, token)
                .post("/auth/logout")
                .then()
                .statusCode(204)
                .header(
                        "Set-Cookie",
                        allOf(containsString(COOKIE + "="), containsString("Max-Age=0")));

        refresh(token).then().statusCode(401);
    }

    @Test
    void deactivatingTheUserRevokesItsSessions() {
        String email = uniqueEmail();
        long userId = registerClient(email);
        String token = login(email).cookie(COOKIE);

        asAdmin()
                .contentType(ContentType.JSON)
                .post("/users/{id}/deactivate", userId)
                .then()
                .statusCode(200);
        asAdmin()
                .contentType(ContentType.JSON)
                .post("/users/{id}/activate", userId)
                .then()
                .statusCode(200);

        refresh(token).then().statusCode(401);
    }

    @Test
    void changingThePasswordRevokesSessionsButKeepingItDoesNot() {
        String email = uniqueEmail();
        long userId = registerClient(email);
        String beforeSameUpdate = login(email).cookie(COOKIE);

        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", PASSWORD))
                .put("/users/{id}", userId)
                .then()
                .statusCode(200);
        String stillValid =
                refresh(beforeSameUpdate).then().statusCode(200).extract().cookie(COOKIE);

        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", "another-secret"))
                .put("/users/{id}", userId)
                .then()
                .statusCode(200);
        refresh(stillValid).then().statusCode(401);
    }

    @Test
    void deletingAUserWithSessionsSucceeds() {
        String email = uniqueEmail();
        long userId = registerClient(email);
        String first = login(email).cookie(COOKIE);
        refresh(first).then().statusCode(200);

        asAdmin().delete("/users/{id}", userId).then().statusCode(204);
        asAdmin().get("/users/{id}", userId).then().statusCode(404);
    }

    @Test
    void loginResponseNeverExposesTheRefreshTokenInTheBody() {
        String email = uniqueEmail();
        registerClient(email);

        Response response = login(email);
        assertFalse(response.asString().contains(response.cookie(COOKIE)));
    }

    @Test
    void corsPreflightAllowsTheFrontOriginWithCredentialsAndTheCsrfHeader() {
        given().header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,x-requested-with")
                .options("/auth/refresh")
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", "http://localhost:5173")
                .header("Access-Control-Allow-Credentials", "true")
                .header("Access-Control-Allow-Headers", containsString("X-Requested-With"));

        given().header("Origin", "https://evil.example.com")
                .header("Access-Control-Request-Method", "POST")
                .options("/auth/refresh")
                .then()
                .header("Access-Control-Allow-Origin", nullValue());
    }

    private static String accessToken(Response session) {
        return session.jsonPath().getString("accessToken");
    }

    private static void logout(String refreshToken) {
        given().contentType(ContentType.JSON)
                .header(CSRF_HEADER, CSRF_VALUE)
                .cookie(COOKIE, refreshToken)
                .post("/auth/logout")
                .then()
                .statusCode(204);
    }

    @Test
    void logoutKillsTheAccessTokensOfThatSessionOnly() {
        String email = uniqueEmail();
        registerClient(email);
        Response laptop = login(email);
        Response phone = login(email);

        logout(laptop.cookie(COOKIE));

        given().auth()
                .oauth2(accessToken(laptop))
                .get("/auth/me")
                .then()
                .statusCode(401)
                .contentType(ContentType.JSON)
                .header("WWW-Authenticate", "Bearer")
                .body("message", is("Invalid or expired token"));
        given().auth().oauth2(accessToken(phone)).get("/auth/me").then().statusCode(200);
    }

    @Test
    void rotatingKeepsTheSessionSoEarlierAccessTokensStillWork() {
        String email = uniqueEmail();
        registerClient(email);
        Response session = login(email);
        Response rotated =
                refresh(session.cookie(COOKIE)).then().statusCode(200).extract().response();

        given().auth().oauth2(accessToken(session)).get("/auth/me").then().statusCode(200);
        given().auth().oauth2(accessToken(rotated)).get("/auth/me").then().statusCode(200);

        logout(rotated.cookie(COOKIE));

        given().auth().oauth2(accessToken(session)).get("/auth/me").then().statusCode(401);
        given().auth().oauth2(accessToken(rotated)).get("/auth/me").then().statusCode(401);
    }

    @Test
    void refreshTokenReuseAlsoKillsTheAccessTokens() {
        String email = uniqueEmail();
        registerClient(email);
        Response session = login(email);
        Response rotated =
                refresh(session.cookie(COOKIE)).then().statusCode(200).extract().response();

        refresh(session.cookie(COOKIE)).then().statusCode(401);

        given().auth().oauth2(accessToken(rotated)).get("/auth/me").then().statusCode(401);
    }

    @Test
    void changingThePasswordKillsTheAccessTokens() {
        String email = uniqueEmail();
        long userId = registerClient(email);
        Response session = login(email);

        asAdmin()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", "another-secret"))
                .put("/users/{id}", userId)
                .then()
                .statusCode(200);

        given().auth().oauth2(accessToken(session)).get("/auth/me").then().statusCode(401);
    }
}

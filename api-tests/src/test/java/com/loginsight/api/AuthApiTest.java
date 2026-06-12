package com.loginsight.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.notNullValue;

/**
 * Contract tests for the authentication flow: obtaining a JWT and using it to
 * reach a protected endpoint. Tagged {@code api} for the same reason as the
 * other live-server suites.
 */
@Tag("api")
class AuthApiTest extends BaseApiTest {

    private static final String ADMIN_CREDENTIALS = """
            {
              "username": "admin",
              "password": "admin123"
            }
            """;

    @Test
    @DisplayName("Logging in with admin credentials returns a JWT")
    void loginWithAdminCredentials_returnsJwt() {
        apiRequest()
                .body(ADMIN_CREDENTIALS)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue());
    }

    @Test
    @DisplayName("Reaching a protected endpoint without a token is rejected (401)")
    void accessProtectedEndpointWithoutToken_returns401() {
        apiRequest()
                .when()
                .get("/api/v1/logs")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("A valid bearer token grants access to the protected endpoint (200)")
    void accessProtectedEndpointWithValidToken_returns200() {
        String token = apiRequest()
                .body(ADMIN_CREDENTIALS)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("token");

        apiRequest()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/logs")
                .then()
                .statusCode(200);
    }
}

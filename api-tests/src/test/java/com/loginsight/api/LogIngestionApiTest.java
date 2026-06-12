package com.loginsight.api;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;


import static org.hamcrest.Matchers.equalTo;

/**
 * Contract tests for the log ingestion endpoints. These run against a live
 * instance at {@code api.base.url}; they are tagged {@code api} so they can be
 * excluded with {@code -Dgroups=!api} when no server is available jji.
 */
@Tag("api")
class LogIngestionApiTest extends BaseApiTest {

    private static final String VALID_LOG = """
            {
              "timestamp": "2026-06-12T10:15:30Z",
              "level": "ERROR",
              "service": "api",
              "message": "Upstream timeout while calling billing"
            }
            """;

    @Test
    @DisplayName("POST /api/v1/logs with a valid payload is accepted (202)")
    void postValidLog_returns202() {
        apiRequest()
                .body(VALID_LOG)
                .when()
                .post("/api/v1/logs")
                .then()
                .statusCode(202);
    }

    @Test
    @DisplayName("POST /api/v1/logs with a malformed payload is rejected (400)")
    void postInvalidLog_returns400() {
        apiRequest()
                .contentType(ContentType.TEXT)
                .body("NOT_VALID_JSON")
                .when()
                .post("/api/v1/logs")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("GET /actuator/health reports the service is UP")
    void getHealthEndpoint_returns200() {
        apiRequest()
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }
}

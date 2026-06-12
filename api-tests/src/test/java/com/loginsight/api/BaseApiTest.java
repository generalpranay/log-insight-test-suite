package com.loginsight.api;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;

import static io.restassured.RestAssured.given;

/**
 * Shared REST Assured setup for the API contract tests. The target base URL is
 * configurable via the {@code api.base.url} system property so the same suite
 * can run against a local instance or a deployed environment.
 */
abstract class BaseApiTest {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080";

    @BeforeAll
    static void configureRestAssured() {
        RestAssured.baseURI = System.getProperty("api.base.url", DEFAULT_BASE_URL);
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
    }

    /**
     * A JSON request specification used as the starting point for every call.
     */
    protected RequestSpecification apiRequest() {
        return given().contentType(ContentType.JSON);
    }
}

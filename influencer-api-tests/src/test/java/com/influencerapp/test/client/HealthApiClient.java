package com.influencerapp.test.client;

import com.influencerapp.test.config.TestConfig;
import io.qameta.allure.Step;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class HealthApiClient {

    private final String baseUrl;

    public HealthApiClient() {
        this.baseUrl = TestConfig.getInstance().getBaseUrl();
    }

    @Step("Get system health check (/api/health or /actuator/health)")
    public Response getHealth() {
        Response response = given()
                .baseUri(baseUrl)
                .when()
                .get("/api/health");
        if (response.statusCode() == 403 || response.statusCode() == 404) {
            response = given()
                    .baseUri(baseUrl)
                    .when()
                    .get("/actuator/health");
        }
        return response;
    }

    @Step("Get system health check with Correlation ID header: {correlationId}")
    public Response getHealthWithCorrelationId(String correlationId) {
        Response response = given()
                .baseUri(baseUrl)
                .header("X-Correlation-Id", correlationId)
                .when()
                .get("/api/health");
        if (response.statusCode() == 403 || response.statusCode() == 404) {
            response = given()
                    .baseUri(baseUrl)
                    .header("X-Correlation-Id", correlationId)
                    .when()
                    .get("/actuator/health");
        }
        return response;
    }
}

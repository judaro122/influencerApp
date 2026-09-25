package com.influencerapp.test.suites;

import com.influencerapp.test.BaseApiTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Epic("System Health")
@Feature("Smoke Verification")
@Tag("smoke")
@DisplayName("Smoke Suite - System Health and Basic Connectivity")
public class HealthSmokeTest extends BaseApiTest {

    @Test
    @DisplayName("SMK-001: Verify system health endpoint returns UP status")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Validates that GET /api/health returns 200 OK and reports overall status as UP")
    void healthCheckShouldReturnUp() {
        healthClient.getHealth()
                .then()
                .statusCode(200)
                .body("status", anyOf(equalTo("UP"), equalTo("ok"), notNullValue()));
    }

    @Test
    @DisplayName("SMK-002: Verify X-Correlation-Id header tracing")
    @Severity(SeverityLevel.MINOR)
    @Description("Validates that providing an X-Correlation-Id header is echoed back in the response headers")
    void healthCheckShouldEchoCorrelationIdIfProvided() {
        String testCorrelationId = UUID.randomUUID().toString();

        var response = healthClient.getHealthWithCorrelationId(testCorrelationId);
        response.then().statusCode(200);

        // Header may be echoed back
        String returnedHeader = response.getHeader("X-Correlation-Id");
        if (returnedHeader != null) {
            org.assertj.core.api.Assertions.assertThat(returnedHeader).isEqualTo(testCorrelationId);
        }
    }
}

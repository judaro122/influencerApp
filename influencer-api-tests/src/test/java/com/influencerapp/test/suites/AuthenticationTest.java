package com.influencerapp.test.suites;

import com.influencerapp.test.BaseApiTest;
import com.influencerapp.test.model.LoginRequest;
import com.influencerapp.test.model.LoginResponse;
import com.influencerapp.test.model.RegisterRequest;
import com.influencerapp.test.utils.TestDataGenerator;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@Epic("Authentication")
@Feature("User Registration & Login")
@Tag("auth")
@Tag("regression")
@DisplayName("Authentication Suite - Registration, Login, and Validation")
public class AuthenticationTest extends BaseApiTest {

    @Test
    @DisplayName("AUTH-001: Register user with valid credentials returns 201 Created")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Registers a new unique user and verifies 201 Created response and Location header")
    void testSuccessfulRegistration() {
        String email = TestDataGenerator.generateUniqueEmail();
        String password = TestDataGenerator.generateValidPassword();

        var response = authClient.register(new RegisterRequest(email, password));

        response.then()
                .statusCode(201)
                .header("Location", containsString("/api/auth/register"));
    }

    @Test
    @DisplayName("AUTH-002: Duplicate registration should be rejected")
    @Severity(SeverityLevel.NORMAL)
    @Description("Attempts to register the same email address twice and expects 400 or 409 error")
    void testDuplicateRegistrationFails() {
        String email = TestDataGenerator.generateUniqueEmail();
        String password = TestDataGenerator.generateValidPassword();

        // First registration
        authClient.register(new RegisterRequest(email, password))
                .then()
                .statusCode(201);

        // Second registration with same email
        authClient.register(new RegisterRequest(email, password))
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(409)));
    }

    @Test
    @DisplayName("AUTH-003: Registration with invalid email format returns 400 Bad Request")
    @Severity(SeverityLevel.NORMAL)
    @Description("Validates that poorly formatted email addresses fail with RFC 7807 field error")
    void testRegistrationWithInvalidEmail() {
        var response = authClient.register(new RegisterRequest("not-a-valid-email", "SecurePassword123!"));

        response.then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("fieldErrors.email", notNullValue());
    }

    @Test
    @DisplayName("AUTH-004: Registration with short password returns 400 Bad Request")
    @Severity(SeverityLevel.NORMAL)
    @Description("Validates password minimum length requirement (>= 8 characters)")
    void testRegistrationWithShortPassword() {
        var response = authClient.register(new RegisterRequest(TestDataGenerator.generateUniqueEmail(), "123"));

        response.then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("fieldErrors.password", containsString("8"));
    }

    @Test
    @DisplayName("AUTH-005: Successful login returns JWT token and tenantId")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Logs in with registered credentials and verifies valid JWT, tenantId, and expiresIn: 3600")
    void testSuccessfulLogin() {
        String email = TestDataGenerator.generateUniqueEmail();
        String password = TestDataGenerator.generateValidPassword();

        authClient.register(new RegisterRequest(email, password)).then().statusCode(201);

        var response = authClient.login(new LoginRequest(email, password));

        LoginResponse loginResponse = response.then()
                .statusCode(200)
                .extract()
                .as(LoginResponse.class);

        assertThat(loginResponse.getAccessToken()).isNotBlank();
        assertThat(loginResponse.getTenantId()).isNotBlank();
        assertThat(loginResponse.getExpiresIn()).isPositive();
    }

    @Test
    @DisplayName("AUTH-006: Login with invalid password returns 400 or 401 error")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Attempts to log in with an incorrect password and verifies failure")
    void testLoginWithInvalidPassword() {
        String email = TestDataGenerator.generateUniqueEmail();
        String password = TestDataGenerator.generateValidPassword();

        authClient.register(new RegisterRequest(email, password)).then().statusCode(201);

        authClient.login(new LoginRequest(email, "WrongPassword999!"))
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(401)));
    }

    @Test
    @DisplayName("AUTH-007: Login with non-existent user returns 400 or 401 error")
    @Severity(SeverityLevel.NORMAL)
    @Description("Attempts to log in with an unregistered email address")
    void testLoginWithNonExistentUser() {
        authClient.login(new LoginRequest("unregistered_" + TestDataGenerator.generateUniqueEmail(), "SomePassword123!"))
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(401)));
    }
}

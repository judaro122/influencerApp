package com.influencerapp.test.client;

import com.influencerapp.test.config.TestConfig;
import com.influencerapp.test.model.LoginRequest;
import com.influencerapp.test.model.LoginResponse;
import com.influencerapp.test.model.RegisterRequest;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class AuthApiClient {

    private final String baseUrl;

    public AuthApiClient() {
        this.baseUrl = TestConfig.getInstance().getBaseUrl();
    }

    @Step("Register new user with email: {request.email}")
    public Response register(RegisterRequest request) {
        return given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/auth/register");
    }

    @Step("Login with email: {request.email}")
    public Response login(LoginRequest request) {
        return given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/auth/login");
    }

    @Step("Helper: Register user and immediately authenticate")
    public LoginResponse registerAndLogin(String email, String password) {
        register(new RegisterRequest(email, password))
                .then()
                .statusCode(201);

        return login(new LoginRequest(email, password))
                .then()
                .statusCode(200)
                .extract()
                .as(LoginResponse.class);
    }
}

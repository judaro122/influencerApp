package com.influencerapp.test;

import com.influencerapp.test.client.AuthApiClient;
import com.influencerapp.test.client.ChannelApiClient;
import com.influencerapp.test.client.HealthApiClient;
import com.influencerapp.test.client.VideoApiClient;
import com.influencerapp.test.config.TestConfig;
import com.influencerapp.test.model.LoginResponse;
import com.influencerapp.test.utils.TestDataGenerator;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;

@Slf4j
public abstract class BaseApiTest {

    protected static TestConfig config;
    protected static AuthApiClient authClient;
    protected static ChannelApiClient channelClient;
    protected static VideoApiClient videoClient;
    protected static HealthApiClient healthClient;

    @BeforeAll
    public static void globalSetup() {
        config = TestConfig.getInstance();
        RestAssured.baseURI = config.getBaseUrl();
        RestAssured.filters(new AllureRestAssured());

        if (log.isDebugEnabled()) {
            RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        }

        authClient = new AuthApiClient();
        channelClient = new ChannelApiClient();
        videoClient = new VideoApiClient();
        healthClient = new HealthApiClient();

        log.info("BaseApiTest initialized targeting {}", config.getBaseUrl());
    }

    /**
     * Helper to create a new unique user and return the authenticated login response containing the JWT.
     */
    protected LoginResponse createAuthenticatedUser() {
        String email = TestDataGenerator.generateUniqueEmail();
        String password = TestDataGenerator.generateValidPassword();
        return authClient.registerAndLogin(email, password);
    }
}

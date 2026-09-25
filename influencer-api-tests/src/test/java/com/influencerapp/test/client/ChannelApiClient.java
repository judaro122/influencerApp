package com.influencerapp.test.client;

import com.influencerapp.test.config.TestConfig;
import com.influencerapp.test.model.ChannelRegistrationRequest;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class ChannelApiClient {

    private final String baseUrl;

    public ChannelApiClient() {
        this.baseUrl = TestConfig.getInstance().getBaseUrl();
    }

    @Step("Register channel: {request.name}")
    public Response registerChannel(String jwtToken, ChannelRegistrationRequest request) {
        var spec = given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON);

        if (jwtToken != null) {
            spec.header("Authorization", "Bearer " + jwtToken);
        }

        return spec.body(request)
                .when()
                .post("/api/channels/register");
    }

    @Step("List channels (page={page}, size={size})")
    public Response listChannels(String jwtToken, int page, int size) {
        var spec = given()
                .baseUri(baseUrl)
                .queryParam("page", page)
                .queryParam("size", size);

        if (jwtToken != null) {
            spec.header("Authorization", "Bearer " + jwtToken);
        }

        return spec.when().get("/api/channels");
    }

    @Step("List channels with default pagination")
    public Response listChannels(String jwtToken) {
        return listChannels(jwtToken, 0, 20);
    }
}

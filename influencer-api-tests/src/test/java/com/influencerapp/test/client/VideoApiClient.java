package com.influencerapp.test.client;

import com.influencerapp.test.config.TestConfig;
import io.qameta.allure.Step;
import io.restassured.response.Response;

import java.io.File;

import static io.restassured.RestAssured.given;

public class VideoApiClient {

    private final String baseUrl;

    public VideoApiClient() {
        this.baseUrl = TestConfig.getInstance().getBaseUrl();
    }

    @Step("Upload video: channelId={channelId}, idempotencyKey={idempotencyKey}")
    public Response uploadVideo(String jwtToken, File file, String channelId, String idempotencyKey) {
        var spec = given()
                .baseUri(baseUrl);

        if (jwtToken != null) {
            spec.header("Authorization", "Bearer " + jwtToken);
        }
        if (idempotencyKey != null) {
            spec.header("Idempotency-Key", idempotencyKey);
        }
        if (file != null) {
            spec.multiPart("file", file);
        }
        if (channelId != null) {
            spec.multiPart("channelId", channelId);
        }

        return spec.when().post("/api/videos/upload");
    }

    @Step("Get video status: videoId={videoId}")
    public Response getVideoStatus(String jwtToken, String videoId) {
        var spec = given()
                .baseUri(baseUrl);

        if (jwtToken != null) {
            spec.header("Authorization", "Bearer " + jwtToken);
        }

        return spec.when().get("/api/videos/" + videoId);
    }

    @Step("List videos (page={page}, size={size})")
    public Response listVideos(String jwtToken, int page, int size) {
        var spec = given()
                .baseUri(baseUrl)
                .queryParam("page", page)
                .queryParam("size", size);

        if (jwtToken != null) {
            spec.header("Authorization", "Bearer " + jwtToken);
        }

        return spec.when().get("/api/videos");
    }

    @Step("List videos with default pagination")
    public Response listVideos(String jwtToken) {
        return listVideos(jwtToken, 0, 20);
    }
}

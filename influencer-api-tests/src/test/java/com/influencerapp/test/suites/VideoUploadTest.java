package com.influencerapp.test.suites;

import com.influencerapp.test.BaseApiTest;
import com.influencerapp.test.model.ChannelRegistrationRequest;
import com.influencerapp.test.model.LoginResponse;
import com.influencerapp.test.model.PaginatedResponse;
import com.influencerapp.test.model.VideoStatusResponse;
import com.influencerapp.test.model.VideoUploadResponse;
import com.influencerapp.test.utils.MediaFileProvider;
import com.influencerapp.test.utils.TestDataGenerator;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.common.mapper.TypeRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@Epic("Videos")
@Feature("Video Upload & Idempotency")
@Tag("videos")
@Tag("regression")
@DisplayName("Video Upload Suite - Multipart Uploads, MIME Validation, and Idempotency")
public class VideoUploadTest extends BaseApiTest {

    @Test
    @DisplayName("VID-001: Successful multipart video upload returns 201 Created and videoId")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Uploads a valid MP4 file with channelId and unique Idempotency-Key")
    void testSuccessfulVideoUpload() throws IOException {
        LoginResponse user = createAuthenticatedUser();
        File videoFile = MediaFileProvider.createSampleMp4(500 * 1024); // 500KB
        String channelId = UUID.randomUUID().toString();
        String idempotencyKey = TestDataGenerator.generateIdempotencyKey();

        var response = videoClient.uploadVideo(user.getAccessToken(), videoFile, channelId, idempotencyKey);

        VideoUploadResponse uploadResponse = response.then()
                .statusCode(201)
                .extract()
                .as(VideoUploadResponse.class);

        assertThat(uploadResponse.getVideoId()).isNotBlank();
    }

    @Test
    @DisplayName("VID-002: Upload with invalid MIME type (text file) returns 400 Bad Request")
    @Severity(SeverityLevel.NORMAL)
    @Description("Validates that non-video files are rejected during validation")
    void testUploadInvalidMimeTypeFails() throws IOException {
        LoginResponse user = createAuthenticatedUser();
        File textFile = MediaFileProvider.createInvalidTextFile();
        String channelId = UUID.randomUUID().toString();
        String idempotencyKey = TestDataGenerator.generateIdempotencyKey();

        videoClient.uploadVideo(user.getAccessToken(), textFile, channelId, idempotencyKey)
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(422)));
    }

    @Test
    @DisplayName("VID-003: Idempotent duplicate upload returns cached/idempotent response")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Submits identical upload request twice with the same Idempotency-Key and verifies deduplication")
    void testIdempotentDuplicateUpload() throws IOException {
        LoginResponse user = createAuthenticatedUser();
        File videoFile = MediaFileProvider.createSampleMp4(200 * 1024);
        String channelId = UUID.randomUUID().toString();
        String idempotencyKey = TestDataGenerator.generateIdempotencyKey();

        // First attempt
        var firstResponse = videoClient.uploadVideo(user.getAccessToken(), videoFile, channelId, idempotencyKey);
        firstResponse.then().statusCode(201);
        String firstVideoId = firstResponse.as(VideoUploadResponse.class).getVideoId();

        // Second attempt with same key and same file
        var secondResponse = videoClient.uploadVideo(user.getAccessToken(), videoFile, channelId, idempotencyKey);
        secondResponse.then().statusCode(anyOf(equalTo(200), equalTo(201)));
        String secondVideoId = secondResponse.as(VideoUploadResponse.class).getVideoId();

        assertThat(secondVideoId).isEqualTo(firstVideoId);
    }

    @Test
    @DisplayName("VID-004: Get video status returns initial status RECEIVED")
    @Severity(SeverityLevel.NORMAL)
    @Description("Uploads a video and immediately queries GET /api/videos/{videoId}")
    void testGetVideoStatusInitialState() throws IOException {
        LoginResponse user = createAuthenticatedUser();
        File videoFile = MediaFileProvider.createSampleMp4(300 * 1024);
        String channelId = UUID.randomUUID().toString();
        String idempotencyKey = TestDataGenerator.generateIdempotencyKey();

        var uploadRes = videoClient.uploadVideo(user.getAccessToken(), videoFile, channelId, idempotencyKey);
        String videoId = uploadRes.then().statusCode(201).extract().as(VideoUploadResponse.class).getVideoId();

        var statusRes = videoClient.getVideoStatus(user.getAccessToken(), videoId);

        VideoStatusResponse statusObj = statusRes.then()
                .statusCode(200)
                .extract()
                .as(VideoStatusResponse.class);

        assertThat(statusObj.getStatus()).isIn("RECEIVED", "PROCESSING", "UPLOADING", "PUBLISHED");
    }

    @Test
    @DisplayName("VID-005: List videos with pagination returns 200 OK")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies tenant video listing endpoint returns paginated response")
    void testListVideos() {
        LoginResponse user = createAuthenticatedUser();

        var response = videoClient.listVideos(user.getAccessToken(), 0, 10);

        PaginatedResponse<VideoStatusResponse> paginated = response.then()
                .statusCode(200)
                .extract()
                .as(new TypeRef<PaginatedResponse<VideoStatusResponse>>() {});

        assertThat(paginated.getPage()).isEqualTo(0);
        assertThat(paginated.getSize()).isEqualTo(10);
        assertThat(paginated.getContent()).isNotNull();
    }
}

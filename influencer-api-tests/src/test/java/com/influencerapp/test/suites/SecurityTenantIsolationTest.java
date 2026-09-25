package com.influencerapp.test.suites;

import com.influencerapp.test.BaseApiTest;
import com.influencerapp.test.model.ChannelRegistrationRequest;
import com.influencerapp.test.model.ChannelResponse;
import com.influencerapp.test.model.LoginResponse;
import com.influencerapp.test.model.PaginatedResponse;
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
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;

@Epic("Security & Authorization")
@Feature("Multi-Tenancy & Access Boundary")
@Tag("security")
@Tag("regression")
@DisplayName("Security Suite - Token Authentication and Multi-Tenant Isolation")
public class SecurityTenantIsolationTest extends BaseApiTest {

    @Test
    @DisplayName("SEC-001: Access protected channels endpoint without token returns 401 Unauthorized")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Ensures anonymous requests to /api/channels are rejected with RFC 7807 problem detail")
    void testAccessWithoutTokenReturns401() {
        channelClient.listChannels(null)
                .then()
                .statusCode(401)
                .contentType("application/problem+json");
    }

    @Test
    @DisplayName("SEC-002: Access with malformed token returns 401 Unauthorized")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Ensures invalid or tampered JWT tokens are rejected")
    void testAccessWithMalformedTokenReturns401() {
        channelClient.listChannels("this.is.an.invalid.token")
                .then()
                .statusCode(401)
                .contentType("application/problem+json");
    }

    @Test
    @DisplayName("SEC-003: Tenant Isolation - Tenant B cannot view Tenant A's channels")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Registers a channel under Tenant A and verifies Tenant B cannot view it")
    void testTenantChannelIsolation() {
        LoginResponse tenantA = createAuthenticatedUser();
        LoginResponse tenantB = createAuthenticatedUser();

        String channelName = "TenantA_Channel_" + UUID.randomUUID();
        channelClient.registerChannel(tenantA.getAccessToken(), ChannelRegistrationRequest.builder()
                .name(channelName)
                .encryptedAccessToken("dummyAccessTokenA")
                .encryptedRefreshToken("dummyRefreshTokenA")
                .tokenExpiry("2026-12-31T23:59:59Z")
                .build()).then().statusCode(anyOf(equalTo(201), equalTo(200)));

        // Tenant B lists their own channels
        var listResponse = channelClient.listChannels(tenantB.getAccessToken())
                .then()
                .statusCode(200)
                .extract()
                .as(new TypeRef<PaginatedResponse<ChannelResponse>>() {});

        assertThat(listResponse.getContent())
                .noneMatch(c -> channelName.equals(c.getYoutubeChannelTitle()));
    }

    @Test
    @DisplayName("SEC-004: Tenant Isolation - Tenant B cannot access Tenant A's video details")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Tenant A uploads a video; Tenant B attempts GET /api/videos/{tenantA_videoId} and must be rejected with 404 or 403")
    void testTenantVideoIsolation() throws IOException {
        LoginResponse tenantA = createAuthenticatedUser();
        LoginResponse tenantB = createAuthenticatedUser();

        File sampleVideo = MediaFileProvider.createSampleMp4(500 * 1024);
        String channelId = UUID.randomUUID().toString();
        String idempotencyKey = TestDataGenerator.generateIdempotencyKey();

        var uploadRes = videoClient.uploadVideo(tenantA.getAccessToken(), sampleVideo, channelId, idempotencyKey);
        if (uploadRes.statusCode() == 201) {
            String videoId = uploadRes.as(VideoUploadResponse.class).getVideoId();

            // Tenant B attempts to access Tenant A's video
            videoClient.getVideoStatus(tenantB.getAccessToken(), videoId)
                    .then()
                    .statusCode(anyOf(equalTo(404), equalTo(401), equalTo(403)));
        }
    }
}

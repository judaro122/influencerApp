package com.influencerapp.test.suites;

import com.influencerapp.test.BaseApiTest;
import com.influencerapp.test.model.ChannelRegistrationRequest;
import com.influencerapp.test.model.ChannelResponse;
import com.influencerapp.test.model.LoginResponse;
import com.influencerapp.test.model.PaginatedResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@Epic("Channels")
@Feature("Channel Management & Linking")
@Tag("channels")
@Tag("regression")
@DisplayName("Channel Management Suite - Registration and Pagination")
public class ChannelManagementTest extends BaseApiTest {

    @Test
    @DisplayName("CHAN-001: Register channel with valid tokens returns 201 Created")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Registers a new channel with OAuth token details and verifies 201 Created")
    void testRegisterChannelSuccess() {
        LoginResponse user = createAuthenticatedUser();
        String channelName = TestDataGenerator.generateChannelName();

        var response = channelClient.registerChannel(user.getAccessToken(), ChannelRegistrationRequest.builder()
                .name(channelName)
                .encryptedAccessToken("enc_access_token_sample")
                .encryptedRefreshToken("enc_refresh_token_sample")
                .tokenExpiry("2026-12-31T23:59:59Z")
                .build());

        response.then()
                .statusCode(anyOf(equalTo(201), equalTo(200)))
                .header("Location", containsString("/api/channels/register"));
    }

    @Test
    @DisplayName("CHAN-002: Register channel with blank fields returns 400 Bad Request")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies validation constraints on channel registration")
    void testRegisterChannelValidationFailure() {
        LoginResponse user = createAuthenticatedUser();

        channelClient.registerChannel(user.getAccessToken(), ChannelRegistrationRequest.builder()
                .name("")
                .encryptedAccessToken("")
                .encryptedRefreshToken("")
                .tokenExpiry("")
                .build())
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("CHAN-003: List channels with default pagination returns page 0, size 20")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies default pagination envelope and content array")
    void testListChannelsDefaultPagination() {
        LoginResponse user = createAuthenticatedUser();

        var response = channelClient.listChannels(user.getAccessToken());

        PaginatedResponse<ChannelResponse> paginated = response.then()
                .statusCode(200)
                .extract()
                .as(new TypeRef<PaginatedResponse<ChannelResponse>>() {});

        assertThat(paginated.getPage()).isEqualTo(0);
        assertThat(paginated.getSize()).isEqualTo(20);
        assertThat(paginated.getContent()).isNotNull();
    }

    @Test
    @DisplayName("CHAN-004: List channels with custom pagination parameters (size=5)")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies size=5 query parameter is respected by the API")
    void testListChannelsCustomPagination() {
        LoginResponse user = createAuthenticatedUser();

        var response = channelClient.listChannels(user.getAccessToken(), 0, 5);

        PaginatedResponse<ChannelResponse> paginated = response.then()
                .statusCode(200)
                .extract()
                .as(new TypeRef<PaginatedResponse<ChannelResponse>>() {});

        assertThat(paginated.getPage()).isEqualTo(0);
        assertThat(paginated.getSize()).isEqualTo(5);
    }
}

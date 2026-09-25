package com.influencerapp.test.utils;

import com.influencerapp.test.client.VideoApiClient;
import com.influencerapp.test.config.TestConfig;
import com.influencerapp.test.model.VideoStatusResponse;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;

import java.time.Duration;

@Slf4j
public final class PollingUtils {

    private PollingUtils() {
    }

    @Step("Poll until video status transitions out of RECEIVED (videoId={videoId})")
    public static VideoStatusResponse pollUntilProcessingOrPublished(VideoApiClient client, String jwtToken, String videoId) {
        TestConfig config = TestConfig.getInstance();
        return Awaitility.await()
                .atMost(Duration.ofSeconds(config.getAsyncPollingSeconds()))
                .pollInterval(Duration.ofSeconds(config.getPollingIntervalSeconds()))
                .until(() -> {
                    var response = client.getVideoStatus(jwtToken, videoId);
                    if (response.statusCode() == 200) {
                        return response.as(VideoStatusResponse.class);
                    }
                    return null;
                }, res -> res != null && !res.getStatus().equals("RECEIVED"));
    }

    @Step("Poll until video is PUBLISHED or FAILED (videoId={videoId})")
    public static VideoStatusResponse pollUntilTerminalState(VideoApiClient client, String jwtToken, String videoId) {
        TestConfig config = TestConfig.getInstance();
        return Awaitility.await()
                .atMost(Duration.ofSeconds(config.getAsyncPollingSeconds()))
                .pollInterval(Duration.ofSeconds(config.getPollingIntervalSeconds()))
                .until(() -> {
                    var response = client.getVideoStatus(jwtToken, videoId);
                    if (response.statusCode() == 200) {
                        var statusObj = response.as(VideoStatusResponse.class);
                        log.debug("Current polled video status for {}: {}", videoId, statusObj.getStatus());
                        return statusObj;
                    }
                    return null;
                }, res -> res != null && ("PUBLISHED".equals(res.getStatus()) || "FAILED".equals(res.getStatus())));
    }
}

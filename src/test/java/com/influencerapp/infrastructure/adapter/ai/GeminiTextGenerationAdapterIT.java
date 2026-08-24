package com.influencerapp.infrastructure.adapter.ai;

import com.influencerapp.domain.model.FileMetadata;
import com.influencerapp.domain.model.VideoMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@SpringBootTest
@AutoConfigureWireMock(port = 0)
/**
 * GeminiTextGenerationAdapterIT component.
 *
 * @author judaro122
 * @since 1.0.0
 */
class GeminiTextGenerationAdapterIT {

    @Autowired
    private GeminiTextGenerationAdapter adapter;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Test
    @DisplayName("should generate title and description from Gemini API")
    void shouldGenerateTitleAndDescription() {
        String mockResponse = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"title\\\": \\\"Test Title\\\", \\\"description\\\": \\\"Test Description\\\"}\"}]}}]}";

        stubFor(post(urlEqualTo("/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)));

        FileMetadata fileMetadata = new FileMetadata("test.mp4", 1024, "video/mp4", "checksum");
        VideoMetadata result = adapter.generateTitleAndDescription(fileMetadata);

        assertNotNull(result);
        assertEquals("Test Title", result.getTitle());
        assertEquals("Test Description", result.getDescription());
    }

    @Test
    @DisplayName("should return fallback when Gemini API fails")
    void shouldReturnFallbackWhenApiFails() {
        stubFor(post(urlMatching("/v1beta/models/gemini-2.0-flash:generateContent?key=.*"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

        FileMetadata fileMetadata = new FileMetadata("test.mp4", 1024, "video/mp4", "checksum");
        VideoMetadata result = adapter.generateTitleAndDescription(fileMetadata);

        assertNotNull(result);
        assertEquals("Title for test.mp4", result.getTitle());
        assertEquals("Description for test.mp4", result.getDescription());
    }
}
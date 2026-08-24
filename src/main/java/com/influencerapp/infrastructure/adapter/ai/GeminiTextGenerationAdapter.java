package com.influencerapp.infrastructure.adapter.ai;

import com.influencerapp.domain.model.FileMetadata;
import com.influencerapp.domain.model.VideoMetadata;
import com.influencerapp.domain.port.outbound.AITextGenerationPort;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

@Component
@RequiredArgsConstructor
/**
 * Adapter implementing AI text generation using Google Gemini with circuit breaker and fallback.
 *
 * @author judaro122
 * @since 1.0.0
 */
public class GeminiTextGenerationAdapter implements AITextGenerationPort {

    @Value("${gemini.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    @Override
    public VideoMetadata generateTitleAndDescription(FileMetadata fileMetadata) {
        String filename = fileMetadata.getFilename();
        try {
            return circuitBreaker.executeSupplier(() ->
                    retry.executeSupplier(() -> callGemini(filename))
            );
        } catch (Exception e) {
            return fallback(filename);
        }
    }

    private VideoMetadata callGemini(String filename) {
        String prompt = "Generate a short title and description for a video file named " + filename + ". Return only a JSON object with title and description fields.";
        String url = GEMINI_URL + apiKey;

        Map<String, Object> requestBody = Map.of(
                "contents", Collections.singletonList(
                        Map.of("parts", Collections.singletonList(
                                Map.of("text", prompt)
                        ))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
        String generatedText = extractText(response.getBody());
        return parseGeneratedText(generatedText, filename);
    }

    private String extractText(Map<String, Object> responseBody) {
        if (responseBody == null) {
            throw new RuntimeException("Empty response from Gemini");
        }
        try {
            java.util.List<Map<String, Object>> candidates = (java.util.List<Map<String, Object>>) responseBody.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new RuntimeException("No candidates in Gemini response");
            }
            Map<String, Object> candidate = candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) candidate.get("content");
            java.util.List<Map<String, Object>> parts = (java.util.List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) {
                throw new RuntimeException("No parts in Gemini response");
            }
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response", e);
        }
    }

    private VideoMetadata parseGeneratedText(String text, String filename) {
        String title = "Title for " + filename;
        String description = "Description for " + filename;

        try {
            String trimmed = text.trim();
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                String json = trimmed.substring(1, trimmed.length() - 1);
                String[] pairs = json.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split(":");
                    if (kv.length == 2) {
                        String key = kv[0].replaceAll("\"", "").trim();
                        String value = kv[1].replaceAll("\"", "").trim();
                        if ("title".equals(key)) {
                            title = value;
                        } else if ("description".equals(key)) {
                            description = value;
                        }
                    }
                }
            } else {
                String[] lines = trimmed.split("\n");
                if (lines.length >= 1) {
                    title = lines[0].trim();
                }
                if (lines.length >= 2) {
                    description = lines[1].trim();
                }
            }
        } catch (Exception e) {
            // Use defaults if parsing fails
        }

        return new VideoMetadata(title, description);
    }

    private VideoMetadata fallback(String filename) {
        return new VideoMetadata("Title for " + filename, "Description for " + filename);
    }
}

package com.influencerapp.test.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoStatusResponse {
    private Object videoId;
    private Object tenantId;
    private Object channelId;
    private String status;
    private Map<String, Object> metadata;
    private Object youtubeVideoId;
    private String youtubeUrl;
    private String errorReason;
    private Instant createdAt;
    private Instant updatedAt;
}

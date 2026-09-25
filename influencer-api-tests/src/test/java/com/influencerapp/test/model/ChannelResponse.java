package com.influencerapp.test.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChannelResponse {
    private Object channelId;
    private String youtubeChannelId;
    private String youtubeChannelTitle;
    private String scope;
    private Instant createdAt;
}

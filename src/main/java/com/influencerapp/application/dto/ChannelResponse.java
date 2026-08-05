package com.influencerapp.application.dto;

import com.influencerapp.domain.model.ChannelId;
import lombok.Data;
import java.time.Instant;

@Data
public class ChannelResponse {

    private ChannelId channelId;
    private String youtubeChannelId;
    private String youtubeChannelTitle;
    private String scope;
    private Instant createdAt;
}
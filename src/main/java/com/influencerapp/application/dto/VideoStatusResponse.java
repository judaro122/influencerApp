package com.influencerapp.application.dto;

import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.VideoId;
import com.influencerapp.domain.model.VideoMetadata;
import com.influencerapp.domain.model.VideoStatus;
import com.influencerapp.domain.model.YouTubeVideoId;
import lombok.Data;
import java.time.Instant;

@Data
public class VideoStatusResponse {

    private VideoId videoId;
    private TenantId tenantId;
    private ChannelId channelId;
    private VideoStatus status;
    private VideoMetadata metadata;
    private YouTubeVideoId youtubeVideoId;
    private String youtubeUrl;
    private String errorReason;
    private Instant createdAt;
    private Instant updatedAt;
}
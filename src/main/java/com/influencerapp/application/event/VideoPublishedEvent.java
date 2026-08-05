package com.influencerapp.application.event;

import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.VideoId;
import com.influencerapp.domain.model.YouTubeVideoId;
import com.influencerapp.domain.model.YouTubeUrl;
import lombok.Value;

@Value
public class VideoPublishedEvent {

    TenantId tenantId;
    VideoId videoId;
    ChannelId channelId;
    YouTubeVideoId youtubeVideoId;
    YouTubeUrl youtubeUrl;
}
package com.influencerapp.application.event;

import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.VideoId;
import com.influencerapp.domain.model.YouTubeVideoId;
import com.influencerapp.domain.model.YouTubeUrl;
import lombok.Value;

@Value
/**
 * Event payload emitted after a video is successfully published to YouTube.
 *
 * @author judaro122
 * @since 1.0.0
 */


public class VideoPublishedEvent {

    TenantId tenantId;
    VideoId videoId;
    ChannelId channelId;
    YouTubeVideoId youtubeVideoId;
    YouTubeUrl youtubeUrl;
}
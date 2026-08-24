package com.influencerapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Value;
import java.time.Instant;

@Value
@AllArgsConstructor
/**
 * Aggregate root representing a video in the publishing pipeline.
 *
 * @author judaro122
 * @since 1.0.0
 */


public class Video {

    VideoId videoId;
    TenantId tenantId;
    ChannelId channelId;
    FileMetadata fileMetadata;
    StoragePath storagePath;
    VideoMetadata metadata;
    VideoStatus status;
    YouTubeVideoId youtubeVideoId;
    String errorReason;
    Instant createdAt;
    Instant updatedAt;
}
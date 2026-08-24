package com.influencerapp.application.event;

import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.FileMetadata;
import com.influencerapp.domain.model.StoragePath;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.VideoId;
import lombok.Value;

@Value
/**
 * Event payload emitted after a video is successfully uploaded and validated.
 *
 * @author judaro122
 * @since 1.0.0
 */


public class VideoReceivedEvent {

    TenantId tenantId;
    VideoId videoId;
    ChannelId channelId;
    StoragePath storagePath;
    FileMetadata fileMetadata;
}
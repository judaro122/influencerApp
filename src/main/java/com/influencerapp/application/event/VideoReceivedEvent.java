package com.influencerapp.application.event;

import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.FileMetadata;
import com.influencerapp.domain.model.StoragePath;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.VideoId;
import lombok.Value;

@Value
public class VideoReceivedEvent {

    TenantId tenantId;
    VideoId videoId;
    ChannelId channelId;
    StoragePath storagePath;
    FileMetadata fileMetadata;
}
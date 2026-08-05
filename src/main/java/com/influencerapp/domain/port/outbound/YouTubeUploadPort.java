package com.influencerapp.domain.port.outbound;

import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.YouTubeVideoId;
import java.io.InputStream;

public interface YouTubeUploadPort {

    YouTubeVideoId upload(TenantId tenantId, ChannelId channelId, InputStream inputStream, String contentType, String filename);
}

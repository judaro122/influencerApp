package com.influencerapp.infrastructure.adapter.youtube;

import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.YouTubeVideoId;
import com.influencerapp.domain.port.outbound.YouTubeUploadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class YouTubeUploadAdapter implements YouTubeUploadPort {

    @Override
    public YouTubeVideoId upload(TenantId tenantId, ChannelId channelId, InputStream inputStream, String contentType, String filename) {
        throw new UnsupportedOperationException("Not implemented in Phase 1");
    }
}

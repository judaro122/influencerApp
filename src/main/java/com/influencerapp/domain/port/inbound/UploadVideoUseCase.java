package com.influencerapp.domain.port.inbound;

import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.VideoId;
import org.springframework.web.multipart.MultipartFile;

public interface UploadVideoUseCase {

    VideoId execute(TenantId tenantId, MultipartFile file, ChannelId channelId);
}

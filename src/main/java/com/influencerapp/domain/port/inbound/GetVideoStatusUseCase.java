package com.influencerapp.domain.port.inbound;

import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.Video;
import com.influencerapp.domain.model.VideoId;

public interface GetVideoStatusUseCase {

    Video execute(TenantId tenantId, VideoId videoId);
}

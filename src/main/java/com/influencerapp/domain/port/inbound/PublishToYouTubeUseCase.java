package com.influencerapp.domain.port.inbound;

import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.VideoId;
import com.influencerapp.domain.model.YouTubeVideoId;

/**
 * Inbound port defining the contract for publishing videos to YouTube.
 *
 * @author judaro122
 * @since 1.0.0
 */
public interface PublishToYouTubeUseCase {

    YouTubeVideoId execute(TenantId tenantId, VideoId videoId);
}

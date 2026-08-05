package com.influencerapp.domain.port.inbound;

import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.Video;
import com.influencerapp.domain.model.VideoId;
/**
 * Inbound port defining the contract for retrieving video publishing status.
 *
 * @author judaro122
 * @since 1.0.0
 */



public interface GetVideoStatusUseCase {

    Video execute(TenantId tenantId, VideoId videoId);
}

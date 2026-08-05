package com.influencerapp.domain.port.outbound;

import com.influencerapp.domain.model.PageResult;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.Video;
import com.influencerapp.domain.model.VideoId;
import java.util.Optional;

public interface VideoRepository {

    Video save(Video video);

    Optional<Video> findByIdAndTenantId(VideoId videoId, TenantId tenantId);

    PageResult<Video> findByTenantId(TenantId tenantId, int page, int size);
}

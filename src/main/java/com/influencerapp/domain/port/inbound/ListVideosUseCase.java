package com.influencerapp.domain.port.inbound;

import com.influencerapp.domain.model.PageResult;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.Video;

/**
 * Use case for listing videos with pagination.
 *
 * @author judaro122
 * @since 1.0.0
 */
public interface ListVideosUseCase {

    /**
     * List videos for a tenant with pagination.
     *
     * @param tenantId the tenant ID
     * @param page the page number (0-based)
     * @param size the page size
     * @return paginated result of videos
     */
    PageResult<Video> execute(TenantId tenantId, int page, int size);
}

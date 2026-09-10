package com.influencerapp.application.service;

import com.influencerapp.domain.model.PageResult;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.Video;
import com.influencerapp.domain.port.inbound.ListVideosUseCase;
import com.influencerapp.domain.port.outbound.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Use case implementation for listing videos with pagination.
 *
 * @author judaro122
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ListVideosUseCaseImpl implements ListVideosUseCase {

    private final VideoRepository videoRepository;

    @Override
    public PageResult<Video> execute(TenantId tenantId, int page, int size) {
        log.info("Listing videos for tenant: {}, page: {}, size: {}", tenantId.getValue(), page, size);
        return videoRepository.findByTenantId(tenantId, page, size);
    }
}

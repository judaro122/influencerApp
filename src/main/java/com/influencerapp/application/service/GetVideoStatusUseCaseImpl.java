package com.influencerapp.application.service;

import com.influencerapp.domain.exception.DomainException;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.Video;
import com.influencerapp.domain.model.VideoId;
import com.influencerapp.domain.port.inbound.GetVideoStatusUseCase;
import com.influencerapp.domain.port.outbound.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetVideoStatusUseCaseImpl implements GetVideoStatusUseCase {

    private final VideoRepository videoRepository;

    @Override
    public Video execute(TenantId tenantId, VideoId videoId) {
        Video video = videoRepository.findByIdAndTenantId(videoId, tenantId)
                .orElseThrow(() -> new DomainException("Video not found"));
        return video;
    }
}

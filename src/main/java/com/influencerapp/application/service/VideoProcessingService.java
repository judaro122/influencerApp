package com.influencerapp.application.service;

import com.influencerapp.domain.exception.DomainException;
import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.Video;
import com.influencerapp.domain.model.VideoId;
import com.influencerapp.domain.model.VideoMetadata;
import com.influencerapp.domain.model.VideoStatus;
import com.influencerapp.domain.port.inbound.GenerateMetadataUseCase;
import com.influencerapp.domain.port.outbound.ChannelRepository;
import com.influencerapp.domain.port.outbound.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service orchestrating the video processing pipeline after upload.
 * Transitions video through PROCESSING and UPLOADING states.
 *
 * @author judaro122
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoProcessingService {

    private final VideoRepository videoRepository;
    private final ChannelRepository channelRepository;
    private final GenerateMetadataUseCase generateMetadataUseCase;

    public void processVideo(TenantId tenantId, VideoId videoId) {
        Video video = videoRepository.findByIdAndTenantId(videoId, tenantId)
                .orElseThrow(() -> new DomainException("Video not found"));

        // Transition to PROCESSING
        video = updateVideoStatus(video, VideoStatus.PROCESSING);
        log.info("Video {} transitioned to PROCESSING", videoId.getValue());

        try {
            // Generate metadata using AI
            VideoMetadata metadata = generateMetadataUseCase.execute(tenantId, video.getFileMetadata());
            video = updateVideoMetadata(video, metadata);
            log.info("Generated metadata for video {}: title='{}'", videoId.getValue(), metadata.getTitle());

            // Transition to UPLOADING
            video = updateVideoStatus(video, VideoStatus.UPLOADING);
            log.info("Video {} transitioned to UPLOADING", videoId.getValue());

            // Save updated video
            videoRepository.save(video);
        } catch (Exception e) {
            log.error("Failed to process video {}", videoId.getValue(), e);
            video = updateVideoStatus(video, VideoStatus.FAILED);
            videoRepository.save(video);
            throw new DomainException("Failed to process video: " + e.getMessage());
        }
    }

    private Video updateVideoStatus(Video video, VideoStatus status) {
        return new Video(
                video.getVideoId(),
                video.getTenantId(),
                video.getChannelId(),
                video.getFileMetadata(),
                video.getStoragePath(),
                video.getMetadata(),
                status,
                video.getYoutubeVideoId(),
                video.getErrorReason(),
                video.getCreatedAt(),
                Instant.now()
        );
    }

    private Video updateVideoMetadata(Video video, VideoMetadata metadata) {
        return new Video(
                video.getVideoId(),
                video.getTenantId(),
                video.getChannelId(),
                video.getFileMetadata(),
                video.getStoragePath(),
                metadata,
                video.getStatus(),
                video.getYoutubeVideoId(),
                video.getErrorReason(),
                video.getCreatedAt(),
                Instant.now()
        );
    }
}

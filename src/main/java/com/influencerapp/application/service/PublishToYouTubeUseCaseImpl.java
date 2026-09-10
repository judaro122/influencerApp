package com.influencerapp.application.service;

import com.influencerapp.domain.exception.DomainException;
import com.influencerapp.domain.model.Channel;
import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.Video;
import com.influencerapp.domain.model.VideoId;
import com.influencerapp.domain.model.VideoStatus;
import com.influencerapp.domain.model.YouTubeUrl;
import com.influencerapp.domain.model.YouTubeVideoId;
import com.influencerapp.domain.port.inbound.PublishToYouTubeUseCase;
import com.influencerapp.domain.port.outbound.ChannelRepository;
import com.influencerapp.domain.port.outbound.KafkaProducerPort;
import com.influencerapp.domain.port.outbound.ObjectStoragePort;
import com.influencerapp.domain.port.outbound.VideoRepository;
import com.influencerapp.domain.port.outbound.YouTubeUploadPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Use case implementation orchestrating YouTube video publishing.
 *
 * @author judaro122
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PublishToYouTubeUseCaseImpl implements PublishToYouTubeUseCase {

    private final VideoRepository videoRepository;
    private final ChannelRepository channelRepository;
    private final YouTubeUploadPort youTubeUploadPort;
    private final ObjectStoragePort objectStoragePort;
    private final KafkaProducerPort kafkaProducerPort;
    private final ObjectMapper objectMapper;

    @Override
    public YouTubeVideoId execute(TenantId tenantId, VideoId videoId) {
        Video video = videoRepository.findByIdAndTenantId(videoId, tenantId)
                .orElseThrow(() -> new DomainException("Video not found"));

        Channel channel = channelRepository.findByIdAndTenantId(video.getChannelId(), tenantId)
                .orElseThrow(() -> new DomainException("Channel not found"));

        try (InputStream inputStream = objectStoragePort.retrieve(video.getStoragePath().getValue())) {
            YouTubeVideoId youtubeVideoId = youTubeUploadPort.upload(
                    tenantId,
                    video.getChannelId(),
                    inputStream,
                    video.getFileMetadata().getMimeType(),
                    video.getFileMetadata().getFilename()
            );

            // Update video with YouTube ID and PUBLISHED status
            Video updatedVideo = new Video(
                    video.getVideoId(),
                    video.getTenantId(),
                    video.getChannelId(),
                    video.getFileMetadata(),
                    video.getStoragePath(),
                    video.getMetadata(),
                    VideoStatus.PUBLISHED,
                    youtubeVideoId,
                    null,
                    video.getCreatedAt(),
                    Instant.now()
            );
            videoRepository.save(updatedVideo);

            // Emit video-published event
            emitVideoPublishedEvent(updatedVideo, youtubeVideoId);

            log.info("Successfully published video {} to YouTube with ID: {}", videoId.getValue(), youtubeVideoId.getValue());
            return youtubeVideoId;
        } catch (Exception e) {
            log.error("Failed to publish video {} to YouTube", videoId.getValue(), e);
            Video failedVideo = new Video(
                    video.getVideoId(),
                    video.getTenantId(),
                    video.getChannelId(),
                    video.getFileMetadata(),
                    video.getStoragePath(),
                    video.getMetadata(),
                    VideoStatus.FAILED,
                    null,
                    e.getMessage(),
                    video.getCreatedAt(),
                    Instant.now()
            );
            videoRepository.save(failedVideo);
            throw new DomainException("Failed to publish video to YouTube: " + e.getMessage());
        }
    }

    private void emitVideoPublishedEvent(Video video, YouTubeVideoId youtubeVideoId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("schemaVersion", "1.0.0");
            payload.put("tenantId", video.getTenantId().getValue());
            payload.put("correlationId", UUID.randomUUID().toString());
            payload.put("videoId", video.getVideoId().getValue());
            payload.put("channelId", video.getChannelId().getValue());
            payload.put("timestamp", Instant.now().toString());
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("youtubeVideoId", youtubeVideoId.getValue());
            eventPayload.put("youtubeUrl", new YouTubeUrl("https://youtube.com/watch?v=" + youtubeVideoId.getValue()).getValue());
            payload.put("payload", eventPayload);

            String eventJson = objectMapper.writeValueAsString(payload);
            kafkaProducerPort.send("video-published", eventJson);
            log.info("Emitted video-published event for video: {}", video.getVideoId().getValue());
        } catch (Exception e) {
            log.error("Failed to emit video-published event for video: {}", video.getVideoId().getValue(), e);
        }
    }
}

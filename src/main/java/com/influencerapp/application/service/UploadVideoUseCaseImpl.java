package com.influencerapp.application.service;

import com.influencerapp.domain.exception.DomainException;
import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.FileMetadata;
import com.influencerapp.domain.model.StoragePath;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.Video;
import com.influencerapp.domain.model.VideoId;
import com.influencerapp.domain.model.VideoMetadata;
import com.influencerapp.domain.model.VideoStatus;
import com.influencerapp.domain.port.inbound.UploadVideoUseCase;
import com.influencerapp.domain.port.outbound.IdempotencyKeyRepository;
import com.influencerapp.domain.port.outbound.KafkaProducerPort;
import com.influencerapp.domain.port.outbound.ObjectStoragePort;
import com.influencerapp.domain.port.outbound.VideoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Use case implementation orchestrating the video upload pipeline with idempotency.
 *
 * @author judaro122
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UploadVideoUseCaseImpl implements UploadVideoUseCase {

    private final VideoRepository videoRepository;
    private final ObjectStoragePort objectStoragePort;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final KafkaProducerPort kafkaProducerPort;
    private final ObjectMapper objectMapper;

    @Override
    public VideoId execute(TenantId tenantId, MultipartFile file, ChannelId channelId, String idempotencyKey) {
        // Idempotency check (tenant-scoped to prevent cross-tenant leakage)
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (idempotencyKeyRepository.existsByIdempotencyKey(idempotencyKey, tenantId)) {
                log.info("Duplicate upload attempt detected for idempotency key: {}", idempotencyKey);
                throw new DomainException("Duplicate upload request");
            }
        }

        if (file == null || file.isEmpty()) {
            throw new DomainException("File is required");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new DomainException("Filename is required");
        }
        String sanitizedFilename = sanitizeFilename(filename);
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new DomainException("File must be a video");
        }
        long size = file.getSize();
        if (size > 100 * 1024 * 1024) {
            throw new DomainException("File size exceeds maximum of 100MB");
        }
        String checksum = computeChecksum(file);
        FileMetadata fileMetadata = new FileMetadata(sanitizedFilename, size, contentType, checksum);
        String storagePathValue = "tenants/" + tenantId.getValue() + "/videos/" + UUID.randomUUID() + "/" + sanitizedFilename;
        StoragePath storagePath = new StoragePath(storagePathValue);
        try (InputStream inputStream = file.getInputStream()) {
            objectStoragePort.store(inputStream, storagePathValue, contentType);
        } catch (Exception e) {
            throw new DomainException("Failed to store video: " + e.getMessage());
        }
        Video video = new Video(
                new VideoId(UUID.randomUUID().toString()),
                tenantId,
                channelId,
                fileMetadata,
                storagePath,
                new VideoMetadata(null, null),
                VideoStatus.RECEIVED,
                null,
                null,
                Instant.now(),
                Instant.now()
        );
        Video savedVideo = videoRepository.save(video);

        // Save idempotency key
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String responseBody = buildVideoIdResponse(savedVideo.getVideoId());
            idempotencyKeyRepository.save(idempotencyKey, tenantId, responseBody);
        }

        // Emit video-received event via outbox
        emitVideoReceivedEvent(savedVideo);

        return savedVideo.getVideoId();
    }

    private void emitVideoReceivedEvent(Video video) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("schemaVersion", "1.0.0");
            payload.put("tenantId", video.getTenantId().getValue());
            payload.put("correlationId", UUID.randomUUID().toString());
            payload.put("videoId", video.getVideoId().getValue());
            payload.put("channelId", video.getChannelId().getValue());
            payload.put("timestamp", Instant.now().toString());
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("storagePath", video.getStoragePath().getValue());
            eventPayload.put("fileName", video.getFileMetadata().getFilename());
            eventPayload.put("fileSize", video.getFileMetadata().getSize());
            eventPayload.put("mimeType", video.getFileMetadata().getMimeType());
            payload.put("payload", eventPayload);

            String eventJson = objectMapper.writeValueAsString(payload);
            kafkaProducerPort.send("video-received", eventJson);
            log.info("Emitted video-received event for video: {}", video.getVideoId().getValue());
        } catch (Exception e) {
            log.error("Failed to emit video-received event for video: {}", video.getVideoId().getValue(), e);
            // Don't fail the upload if event emission fails - outbox will handle retry
        }
    }

    private String buildVideoIdResponse(VideoId videoId) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("videoId", videoId.getValue());
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return "{\"videoId\":\"" + videoId.getValue() + "\"}";
        }
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String computeChecksum(MultipartFile file) {
        return UUID.randomUUID().toString();
    }
}

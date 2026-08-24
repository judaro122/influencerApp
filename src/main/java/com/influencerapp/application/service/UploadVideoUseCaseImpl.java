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
import com.influencerapp.domain.port.outbound.ObjectStoragePort;
import com.influencerapp.domain.port.outbound.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
/**
 * Use case implementation orchestrating the video upload pipeline.
 *
 * @author judaro122
 * @since 1.0.0
 */


public class UploadVideoUseCaseImpl implements UploadVideoUseCase {

    private final VideoRepository videoRepository;
    private final ObjectStoragePort objectStoragePort;

    @Override
    public VideoId execute(TenantId tenantId, MultipartFile file, ChannelId channelId) {
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
        videoRepository.save(video);
        return video.getVideoId();
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String computeChecksum(MultipartFile file) {
        return UUID.randomUUID().toString();
    }
}

package com.influencerapp.infrastructure.repository;

import com.influencerapp.domain.model.PageResult;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.Video;
import com.influencerapp.domain.model.VideoId;
import com.influencerapp.domain.port.outbound.VideoRepository;
import com.influencerapp.infrastructure.entity.VideoEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VideoRepositoryImpl implements VideoRepository {

    private final JpaVideoRepository jpaVideoRepository;

    @Override
    public Video save(Video video) {
        VideoEntity entity = toEntity(video);
        VideoEntity saved = jpaVideoRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Video> findByIdAndTenantId(VideoId videoId, TenantId tenantId) {
        return jpaVideoRepository.findByVideoIdAndTenantId(videoId.getValue(), tenantId.getValue())
                .map(this::toDomain);
    }

    @Override
    public PageResult<Video> findByTenantId(TenantId tenantId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<VideoEntity> entityPage = jpaVideoRepository.findByTenantId(tenantId.getValue(), pageRequest);
        return new PageResult<>(
                entityPage.getContent().stream().map(this::toDomain).toList(),
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages()
        );
    }

    private Video toDomain(VideoEntity entity) {
        return new Video(
                new VideoId(entity.getVideoId()),
                new TenantId(entity.getTenantId()),
                new com.influencerapp.domain.model.ChannelId(entity.getChannelId()),
                new com.influencerapp.domain.model.FileMetadata(
                        entity.getFilename(),
                        entity.getFileSize(),
                        entity.getMimeType(),
                        entity.getChecksum()
                ),
                new com.influencerapp.domain.model.StoragePath(entity.getStoragePath()),
                new com.influencerapp.domain.model.VideoMetadata(
                        entity.getTitle(),
                        entity.getDescription()
                ),
                entity.getStatus(),
                entity.getYoutubeVideoId() != null ? new com.influencerapp.domain.model.YouTubeVideoId(entity.getYoutubeVideoId()) : null,
                entity.getErrorReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private VideoEntity toEntity(Video video) {
        VideoEntity entity = new VideoEntity();
        entity.setVideoId(video.getVideoId().getValue());
        entity.setTenantId(video.getTenantId().getValue());
        entity.setChannelId(video.getChannelId().getValue());
        entity.setFilename(video.getFileMetadata().getFilename());
        entity.setFileSize(video.getFileMetadata().getSize());
        entity.setMimeType(video.getFileMetadata().getMimeType());
        entity.setChecksum(video.getFileMetadata().getChecksum());
        entity.setStoragePath(video.getStoragePath().getValue());
        entity.setTitle(video.getMetadata().getTitle());
        entity.setDescription(video.getMetadata().getDescription());
        entity.setStatus(video.getStatus());
        entity.setYoutubeVideoId(video.getYoutubeVideoId() != null ? video.getYoutubeVideoId().getValue() : null);
        entity.setErrorReason(video.getErrorReason());
        entity.setCreatedAt(video.getCreatedAt());
        entity.setUpdatedAt(video.getUpdatedAt());
        return entity;
    }
}

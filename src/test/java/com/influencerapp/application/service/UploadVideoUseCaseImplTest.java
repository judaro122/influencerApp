package com.influencerapp.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.influencerapp.domain.exception.DomainException;
import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.FileMetadata;
import com.influencerapp.domain.model.StoragePath;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.Video;
import com.influencerapp.domain.model.VideoId;
import com.influencerapp.domain.model.VideoMetadata;
import com.influencerapp.domain.model.VideoStatus;
import com.influencerapp.domain.port.outbound.IdempotencyKeyRepository;
import com.influencerapp.domain.port.outbound.KafkaProducerPort;
import com.influencerapp.domain.port.outbound.ObjectStoragePort;
import com.influencerapp.domain.port.outbound.VideoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UploadVideoUseCaseImpl Tests")
class UploadVideoUseCaseImplTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private ObjectStoragePort objectStoragePort;

    @Mock
    private KafkaProducerPort kafkaProducerPort;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private UploadVideoUseCaseImpl uploadVideoUseCase;

    @Test
    @DisplayName("Should upload video successfully and emit video-received event")
    void shouldUploadVideoSuccessfully() throws Exception {
        // Given
        TenantId tenantId = new TenantId("tenant-123");
        ChannelId channelId = new ChannelId("channel-456");
        VideoId savedVideoId = new VideoId("video-123");
        MultipartFile file = new MockMultipartFile("file", "test-video.mp4", "video/mp4", "test content".getBytes());

        Video savedVideo = new Video(
                savedVideoId,
                tenantId,
                channelId,
                new FileMetadata("test-video.mp4", 12L, "video/mp4", "checksum"),
                new StoragePath("tenants/tenant-123/videos/video-123/test-video.mp4"),
                new VideoMetadata(null, null),
                VideoStatus.RECEIVED,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"videoId\":\"" + savedVideoId.getValue() + "\"}");
        when(videoRepository.save(any())).thenReturn(savedVideo);

        // When
        VideoId result = uploadVideoUseCase.execute(tenantId, file, channelId, null);

        // Then
        assertThat(result).isEqualTo(savedVideoId);
        verify(videoRepository).save(any());
        verify(objectStoragePort).store(any(), any(), any());
        verify(kafkaProducerPort).send(eq("video-received"), any());
    }

    @Test
    @DisplayName("Should reject duplicate upload with same idempotency key")
    void shouldRejectDuplicateUpload() {
        // Given
        TenantId tenantId = new TenantId("tenant-123");
        ChannelId channelId = new ChannelId("channel-456");
        MultipartFile file = new MockMultipartFile("file", "test-video.mp4", "video/mp4", "test content".getBytes());

        when(idempotencyKeyRepository.existsByIdempotencyKey("duplicate-key", tenantId)).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> uploadVideoUseCase.execute(tenantId, file, channelId, "duplicate-key"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Duplicate upload request");

        verify(videoRepository, never()).save(any());
        verify(objectStoragePort, never()).store(any(), any(), any());
    }

    @Test
    @DisplayName("Should reject non-video file type")
    void shouldRejectNonVideoFile() {
        // Given
        TenantId tenantId = new TenantId("tenant-123");
        ChannelId channelId = new ChannelId("channel-456");
        MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test content".getBytes());

        // When / Then
        assertThatThrownBy(() -> uploadVideoUseCase.execute(tenantId, file, channelId, null))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("File must be a video");
    }

    @Test
    @DisplayName("Should reject file exceeding size limit")
    void shouldRejectOversizedFile() {
        // Given
        TenantId tenantId = new TenantId("tenant-123");
        ChannelId channelId = new ChannelId("channel-456");
        byte[] largeContent = new byte[101 * 1024 * 1024]; // 101MB
        MultipartFile file = new MockMultipartFile("file", "test-video.mp4", "video/mp4", largeContent);

        // When / Then
        assertThatThrownBy(() -> uploadVideoUseCase.execute(tenantId, file, channelId, null))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("File size exceeds maximum");
    }
}

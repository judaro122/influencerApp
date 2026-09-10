package com.influencerapp.application.service;

import com.influencerapp.domain.model.Channel;
import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.EncryptedTokens;
import com.influencerapp.domain.model.StoragePath;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.Video;
import com.influencerapp.domain.model.VideoId;
import com.influencerapp.domain.model.VideoMetadata;
import com.influencerapp.domain.model.VideoStatus;
import com.influencerapp.domain.model.YouTubeVideoId;
import com.influencerapp.domain.port.outbound.ChannelRepository;
import com.influencerapp.domain.port.outbound.KafkaProducerPort;
import com.influencerapp.domain.port.outbound.ObjectStoragePort;
import com.influencerapp.domain.port.outbound.VideoRepository;
import com.influencerapp.domain.port.outbound.YouTubeUploadPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PublishToYouTubeUseCaseImpl}.
 *
 * @author judaro122
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class PublishToYouTubeUseCaseImplTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private YouTubeUploadPort youTubeUploadPort;

    @Mock
    private ObjectStoragePort objectStoragePort;

    @Mock
    private KafkaProducerPort kafkaProducerPort;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private PublishToYouTubeUseCaseImpl publishToYouTubeUseCase;

    @BeforeEach
    void setUp() {
        publishToYouTubeUseCase = new PublishToYouTubeUseCaseImpl(
                videoRepository,
                channelRepository,
                youTubeUploadPort,
                objectStoragePort,
                kafkaProducerPort,
                objectMapper
        );
    }

    @Test
    @DisplayName("Should publish video to YouTube successfully")
    void shouldPublishVideoSuccessfully() throws Exception {
        // Given
        TenantId tenantId = new TenantId("tenant-123");
        VideoId videoId = new VideoId("video-" + UUID.randomUUID());
        ChannelId channelId = new ChannelId("channel-456");

        Channel channel = new Channel(
                new ChannelId("channel-456"),
                tenantId,
                "yt-channel-123",
                "My Channel",
                new EncryptedTokens(new byte[]{1, 2, 3}, new byte[]{4, 5, 6}, "2025-12-31T23:59:59Z"),
                "youtube.upload",
                Instant.now(),
                Instant.now()
        );

        Video video = new Video(
                videoId,
                tenantId,
                channelId,
                new com.influencerapp.domain.model.FileMetadata("test-video.mp4", 1024L, "video/mp4", "checksum"),
                new StoragePath("tenants/tenant-123/videos/" + videoId.getValue() + "/test-video.mp4"),
                new VideoMetadata(null, null),
                VideoStatus.RECEIVED,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        when(videoRepository.findByIdAndTenantId(videoId, tenantId)).thenReturn(Optional.of(video));
        when(channelRepository.findByIdAndTenantId(channelId, tenantId)).thenReturn(Optional.of(channel));
        when(objectStoragePort.retrieve(any())).thenReturn(InputStream.nullInputStream());
        when(youTubeUploadPort.upload(any(), any(), any(), any(), any())).thenReturn(new YouTubeVideoId("yt-video-123"));
        when(videoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        YouTubeVideoId result = publishToYouTubeUseCase.execute(tenantId, videoId);

        // Then
        assertThat(result).isEqualTo(new YouTubeVideoId("yt-video-123"));
        verify(youTubeUploadPort).upload(any(), any(), any(), any(), any());
        verify(kafkaProducerPort).send(eq("video-published"), any());
    }

    @Test
    @DisplayName("Should throw exception when video not found")
    void shouldThrowExceptionWhenVideoNotFound() {
        // Given
        TenantId tenantId = new TenantId("tenant-123");
        VideoId videoId = new VideoId("video-nonexistent");

        when(videoRepository.findByIdAndTenantId(videoId, tenantId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> publishToYouTubeUseCase.execute(tenantId, videoId))
                .isInstanceOf(com.influencerapp.domain.exception.DomainException.class)
                .hasMessageContaining("Video not found");

        verify(youTubeUploadPort, never()).upload(any(), any(), any(), any(), any());
        verify(kafkaProducerPort, never()).send(any(), any());
    }
}

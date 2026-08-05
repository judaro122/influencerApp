package com.influencerapp.application.service;

import com.influencerapp.domain.exception.DomainException;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.Video;
import com.influencerapp.domain.model.VideoId;
import com.influencerapp.domain.port.outbound.VideoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetVideoStatusUseCase")
/**
 * GetVideoStatusUseCaseImplTest component.
 *
 * @author judaro122
 * @since 1.0.0
 */


class GetVideoStatusUseCaseImplTest {

    @Mock
    private VideoRepository videoRepository;

    @InjectMocks
    private GetVideoStatusUseCaseImpl useCase;

    @Test
    @DisplayName("should return video when found")
    void shouldReturnVideoWhenFound() {
        Video video = new Video(new VideoId("video-1"), new TenantId("tenant-1"), null, null, null, null, null, null, null, null, null);
        when(videoRepository.findByIdAndTenantId(new VideoId("video-1"), new TenantId("tenant-1")))
                .thenReturn(Optional.of(video));

        Video result = useCase.execute(new TenantId("tenant-1"), new VideoId("video-1"));

        assertNotNull(result);
        assertEquals("video-1", result.getVideoId().getValue());
    }

    @Test
    @DisplayName("should throw exception when video not found")
    void shouldThrowWhenVideoNotFound() {
        when(videoRepository.findByIdAndTenantId(any(), any())).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> useCase.execute(new TenantId("tenant-1"), new VideoId("video-1")));
    }
}

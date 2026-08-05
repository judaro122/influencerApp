package com.influencerapp.application.service;

import com.influencerapp.domain.exception.DomainException;
import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.FileMetadata;
import com.influencerapp.domain.model.StoragePath;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.VideoId;
import com.influencerapp.domain.port.outbound.ObjectStoragePort;
import com.influencerapp.domain.port.outbound.VideoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UploadVideoUseCase")
class UploadVideoUseCaseImplTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private ObjectStoragePort objectStoragePort;

    @Mock
    private MultipartFile file;

    @InjectMocks
    private UploadVideoUseCaseImpl useCase;

    @Test
    @DisplayName("should upload video and return videoId")
    void shouldUploadVideo() throws Exception {
        when(file.getOriginalFilename()).thenReturn("test.mp4");
        when(file.getContentType()).thenReturn("video/mp4");
        when(file.getSize()).thenReturn(1024L);
        when(file.isEmpty()).thenReturn(false);
        when(file.getInputStream()).thenReturn(InputStream.nullInputStream());
        when(videoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        VideoId result = useCase.execute(new TenantId("tenant-1"), file, new ChannelId("channel-1"));

        assertNotNull(result);
        verify(objectStoragePort).store(any(), any(), any());
        verify(videoRepository).save(any());
    }

    @Test
    @DisplayName("should throw exception when file is null")
    void shouldThrowWhenFileNull() {
        assertThrows(DomainException.class, () -> useCase.execute(new TenantId("tenant-1"), null, new ChannelId("channel-1")));
    }

    @Test
    @DisplayName("should throw exception when file is empty")
    void shouldThrowWhenFileEmpty() throws Exception {
        when(file.isEmpty()).thenReturn(true);

        assertThrows(DomainException.class, () -> useCase.execute(new TenantId("tenant-1"), file, new ChannelId("channel-1")));
    }

    @Test
    @DisplayName("should throw exception when filename is blank")
    void shouldThrowWhenFilenameBlank() throws Exception {
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("");

        assertThrows(DomainException.class, () -> useCase.execute(new TenantId("tenant-1"), file, new ChannelId("channel-1")));
    }

    @Test
    @DisplayName("should throw exception when content type is not video")
    void shouldThrowWhenNotVideo() throws Exception {
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.txt");
        when(file.getContentType()).thenReturn("text/plain");
        // No need to stub getSize since the exception is thrown before it's called

        assertThrows(DomainException.class, () -> useCase.execute(new TenantId("tenant-1"), file, new ChannelId("channel-1")));
    }

    @Test
    @DisplayName("should throw exception when file size exceeds limit")
    void shouldThrowWhenFileTooLarge() throws Exception {
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.mp4");
        when(file.getContentType()).thenReturn("video/mp4");
        when(file.getSize()).thenReturn(200 * 1024 * 1024L);
        // No need to stub getInputStream since the exception is thrown before it's called

        assertThrows(DomainException.class, () -> useCase.execute(new TenantId("tenant-1"), file, new ChannelId("channel-1")));
    }
}

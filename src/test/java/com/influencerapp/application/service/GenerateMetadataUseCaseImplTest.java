package com.influencerapp.application.service;

import com.influencerapp.domain.model.FileMetadata;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.VideoMetadata;
import com.influencerapp.domain.port.outbound.AITextGenerationPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GenerateMetadataUseCase")
class GenerateMetadataUseCaseImplTest {

    @Mock
    private AITextGenerationPort aiTextGenerationPort;

    @InjectMocks
    private GenerateMetadataUseCaseImpl useCase;

    @Test
    @DisplayName("should delegate to AI port")
    void shouldDelegateToAIPort() {
        FileMetadata fileMetadata = new FileMetadata("test.mp4", 1024, "video/mp4", "checksum");
        VideoMetadata expected = new VideoMetadata("Title", "Description");
        when(aiTextGenerationPort.generateTitleAndDescription(fileMetadata)).thenReturn(expected);

        VideoMetadata result = useCase.execute(new TenantId("tenant-1"), fileMetadata);

        assertNotNull(result);
        assertEquals("Title", result.getTitle());
        assertEquals("Description", result.getDescription());
        verify(aiTextGenerationPort).generateTitleAndDescription(fileMetadata);
    }
}

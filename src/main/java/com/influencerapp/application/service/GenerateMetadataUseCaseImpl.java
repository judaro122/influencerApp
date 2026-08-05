package com.influencerapp.application.service;

import com.influencerapp.domain.model.FileMetadata;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.VideoMetadata;
import com.influencerapp.domain.port.inbound.GenerateMetadataUseCase;
import com.influencerapp.domain.port.outbound.AITextGenerationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GenerateMetadataUseCaseImpl implements GenerateMetadataUseCase {

    private final AITextGenerationPort aiTextGenerationPort;

    @Override
    public VideoMetadata execute(TenantId tenantId, FileMetadata fileMetadata) {
        return aiTextGenerationPort.generateTitleAndDescription(fileMetadata);
    }
}

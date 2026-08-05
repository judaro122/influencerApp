package com.influencerapp.infrastructure.adapter.ai;

import com.influencerapp.domain.model.FileMetadata;
import com.influencerapp.domain.model.VideoMetadata;
import com.influencerapp.domain.port.outbound.AITextGenerationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeminiTextGenerationAdapter implements AITextGenerationPort {

    @Override
    public VideoMetadata generateTitleAndDescription(FileMetadata fileMetadata) {
        throw new UnsupportedOperationException("Not implemented in Phase 1");
    }
}

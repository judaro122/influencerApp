package com.influencerapp.domain.port.outbound;

import com.influencerapp.domain.model.FileMetadata;
import com.influencerapp.domain.model.VideoMetadata;
/**
 * Outbound port defining the contract for AI-powered text generation.
 *
 * @author judaro122
 * @since 1.0.0
 */



public interface AITextGenerationPort {

    VideoMetadata generateTitleAndDescription(FileMetadata fileMetadata);
}

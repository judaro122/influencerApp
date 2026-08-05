package com.influencerapp.domain.port.outbound;

import com.influencerapp.domain.model.FileMetadata;
import com.influencerapp.domain.model.VideoMetadata;

public interface AITextGenerationPort {

    VideoMetadata generateTitleAndDescription(FileMetadata fileMetadata);
}

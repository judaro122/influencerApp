package com.influencerapp.domain.port.inbound;

import com.influencerapp.domain.model.FileMetadata;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.VideoMetadata;

public interface GenerateMetadataUseCase {

    VideoMetadata execute(TenantId tenantId, FileMetadata fileMetadata);
}

package com.influencerapp.domain.port.inbound;

import com.influencerapp.domain.model.FileMetadata;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.VideoMetadata;
/**
 * Inbound port defining the contract for AI metadata generation.
 *
 * @author judaro122
 * @since 1.0.0
 */



public interface GenerateMetadataUseCase {

    VideoMetadata execute(TenantId tenantId, FileMetadata fileMetadata);
}

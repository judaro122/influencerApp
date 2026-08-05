package com.influencerapp.domain.port.inbound;

import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.VideoId;
import org.springframework.web.multipart.MultipartFile;
/**
 * Inbound port defining the contract for video upload orchestration.
 *
 * @author judaro122
 * @since 1.0.0
 */



public interface UploadVideoUseCase {

    VideoId execute(TenantId tenantId, MultipartFile file, ChannelId channelId);
}

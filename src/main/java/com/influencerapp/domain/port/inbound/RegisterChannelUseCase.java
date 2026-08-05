package com.influencerapp.domain.port.inbound;

import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.TenantId;

public interface RegisterChannelUseCase {

    ChannelId execute(TenantId tenantId, String authCode);
}

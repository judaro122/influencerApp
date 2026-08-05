package com.influencerapp.domain.port.inbound;

import com.influencerapp.domain.model.Channel;
import com.influencerapp.domain.model.PageResult;
import com.influencerapp.domain.model.TenantId;

public interface ListChannelsUseCase {

    PageResult<Channel> execute(TenantId tenantId, int page, int size);
}

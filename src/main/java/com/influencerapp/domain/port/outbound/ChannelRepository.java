package com.influencerapp.domain.port.outbound;

import com.influencerapp.domain.model.Channel;
import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.PageResult;
import com.influencerapp.domain.model.TenantId;
import java.util.Optional;

public interface ChannelRepository {

    Channel save(Channel channel);

    Optional<Channel> findByIdAndTenantId(ChannelId channelId, TenantId tenantId);

    PageResult<Channel> findByTenantId(TenantId tenantId, int page, int size);
}

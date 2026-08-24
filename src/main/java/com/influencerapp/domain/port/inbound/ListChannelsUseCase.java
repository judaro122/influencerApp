package com.influencerapp.domain.port.inbound;

import com.influencerapp.domain.model.Channel;
import com.influencerapp.domain.model.PageResult;
import com.influencerapp.domain.model.TenantId;
/**
 * Inbound port defining the contract for listing tenant channels.
 *
 * @author judaro122
 * @since 1.0.0
 */



public interface ListChannelsUseCase {

    PageResult<Channel> execute(TenantId tenantId, int page, int size);
}

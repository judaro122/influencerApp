package com.influencerapp.application.service;

import com.influencerapp.domain.model.Channel;
import com.influencerapp.domain.model.PageResult;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.port.inbound.ListChannelsUseCase;
import com.influencerapp.domain.port.outbound.ChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListChannelsUseCaseImpl implements ListChannelsUseCase {

    private final ChannelRepository channelRepository;

    @Override
    public PageResult<Channel> execute(TenantId tenantId, int page, int size) {
        return channelRepository.findByTenantId(tenantId, page, size);
    }
}

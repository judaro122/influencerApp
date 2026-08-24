package com.influencerapp.application.service;

import com.influencerapp.domain.model.Channel;
import com.influencerapp.domain.model.PageResult;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.port.outbound.ChannelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListChannelsUseCase")
/**
 * ListChannelsUseCaseImplTest component.
 *
 * @author judaro122
 * @since 1.0.0
 */


class ListChannelsUseCaseImplTest {

    @Mock
    private ChannelRepository channelRepository;

    @InjectMocks
    private ListChannelsUseCaseImpl useCase;

    @Test
    @DisplayName("should return paginated channels")
    void shouldReturnPaginatedChannels() {
        PageResult<Channel> pageResult = new PageResult<>(java.util.List.of(), 0, 10, 0, 0);
        when(channelRepository.findByTenantId(any(), anyInt(), anyInt())).thenReturn(pageResult);

        PageResult<Channel> result = useCase.execute(new TenantId("tenant-1"), 0, 10);

        assertNotNull(result);
        verify(channelRepository).findByTenantId(any(), anyInt(), anyInt());
    }
}

package com.influencerapp.infrastructure.repository;

import com.influencerapp.domain.model.Channel;
import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.EncryptedTokens;
import com.influencerapp.domain.model.TenantId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class ChannelRepositoryImplTest extends RepositoryIntegrationTestBase {

    @Autowired
    private ChannelRepositoryImpl channelRepository;

    @Test
    @DisplayName("should save and retrieve channel")
    void shouldSaveAndRetrieveChannel() {
        Channel channel = new Channel(
                new ChannelId("channel-1"),
                new TenantId("tenant-1"),
                "youtube-channel-1",
                "Channel 1",
                new EncryptedTokens("encrypted-access".getBytes(), "encrypted-refresh".getBytes(), Instant.now().toString()),
                "https://www.googleapis.com/auth/youtube.upload",
                Instant.now(),
                Instant.now()
        );

        ChannelId savedId = channelRepository.save(channel).getChannelId();
        Channel found = channelRepository.findByIdAndTenantId(savedId, new TenantId("tenant-1")).orElseThrow();

        assertEquals("channel-1", found.getChannelId().getValue());
        assertEquals("tenant-1", found.getTenantId().getValue());
        assertEquals("youtube-channel-1", found.getYoutubeChannelId());
        assertEquals("Channel 1", found.getYoutubeChannelTitle());
    }

    @Test
    @DisplayName("should return empty when channel not found")
    void shouldReturnEmptyWhenNotFound() {
        assertTrue(channelRepository.findByIdAndTenantId(new ChannelId("non-existent"), new TenantId("tenant-1")).isEmpty());
    }

    @Test
    @DisplayName("should list channels by tenant with pagination")
    void shouldListChannelsByTenant() {
        for (int i = 0; i < 3; i++) {
            Channel channel = new Channel(
                    new ChannelId("channel-" + i),
                    new TenantId("tenant-1"),
                    "youtube-channel-" + i,
                    "Channel " + i,
                    new EncryptedTokens("encrypted-access".getBytes(), "encrypted-refresh".getBytes(), Instant.now().toString()),
                    "https://www.googleapis.com/auth/youtube.upload",
                    Instant.now(),
                    Instant.now()
            );
            channelRepository.save(channel);
        }

        var page = channelRepository.findByTenantId(new TenantId("tenant-1"), 0, 2);

        assertEquals(2, page.getContent().size());
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
    }
}

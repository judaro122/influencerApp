package com.influencerapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Value;
import java.time.Instant;

@Value
@AllArgsConstructor
public class Channel {

    ChannelId channelId;
    TenantId tenantId;
    String youtubeChannelId;
    String youtubeChannelTitle;
    EncryptedTokens encryptedTokens;
    String scope;
    Instant createdAt;
    Instant updatedAt;
}
package com.influencerapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Value;
import java.time.Instant;

@Value
@AllArgsConstructor
/**
 * Aggregate root representing a linked YouTube channel for a tenant.
 *
 * @author judaro122
 * @since 1.0.0
 */


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
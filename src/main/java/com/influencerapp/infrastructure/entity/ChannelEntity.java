package com.influencerapp.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "channels")
@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class ChannelEntity {

    @Id
    @Column(name = "channel_id", nullable = false, length = 255)
    private String channelId;

    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;

    @Column(name = "youtube_channel_id", nullable = false, length = 255)
    private String youtubeChannelId;

    @Column(name = "youtube_channel_title", nullable = false, length = 500)
    private String youtubeChannelTitle;

    @Column(name = "access_token_enc", nullable = false)
    private byte[] accessTokenEnc;

    @Column(name = "refresh_token_enc", nullable = false)
    private byte[] refreshTokenEnc;

    @Column(name = "token_expiry", nullable = false)
    private Instant tokenExpiry;

    @Column(name = "scope", nullable = false, length = 255)
    private String scope;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

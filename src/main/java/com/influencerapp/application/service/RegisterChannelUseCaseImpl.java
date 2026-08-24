package com.influencerapp.application.service;

import com.influencerapp.domain.exception.DomainException;
import com.influencerapp.domain.model.Channel;
import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.EncryptedTokens;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.port.inbound.RegisterChannelUseCase;
import com.influencerapp.domain.port.outbound.ChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Use case implementation orchestrating YouTube channel registration.
 *
 * @author judaro122
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class RegisterChannelUseCaseImpl implements RegisterChannelUseCase {

    private final ChannelRepository channelRepository;

    @Override
    public ChannelId execute(
            TenantId tenantId,
            String name,
            String encryptedAccessToken,
            String encryptedRefreshToken,
            String tokenExpiry
    ) {
        if (name == null || name.isBlank()) {
            throw new DomainException("Channel name is required");
        }
        if (encryptedAccessToken == null || encryptedAccessToken.isBlank()) {
            throw new DomainException("Encrypted access token is required");
        }
        if (encryptedRefreshToken == null || encryptedRefreshToken.isBlank()) {
            throw new DomainException("Encrypted refresh token is required");
        }
        if (tokenExpiry == null || tokenExpiry.isBlank()) {
            throw new DomainException("Token expiry is required");
        }

        String channelId = UUID.randomUUID().toString();
        String youtubeChannelId = "youtube-channel-" + UUID.randomUUID();

        byte[] accessTokenEnc = encryptedAccessToken.getBytes(StandardCharsets.UTF_8);
        byte[] refreshTokenEnc = encryptedRefreshToken.getBytes(StandardCharsets.UTF_8);

        Channel channel = new Channel(
                new ChannelId(channelId),
                tenantId,
                youtubeChannelId,
                name,
                new EncryptedTokens(accessTokenEnc, refreshTokenEnc, tokenExpiry),
                "https://www.googleapis.com/auth/youtube.upload",
                Instant.now(),
                Instant.now()
        );
        channelRepository.save(channel);
        return new ChannelId(channelId);
    }
}

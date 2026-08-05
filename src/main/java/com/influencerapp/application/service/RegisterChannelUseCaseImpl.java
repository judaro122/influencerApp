package com.influencerapp.application.service;

import com.influencerapp.domain.exception.DomainException;
import com.influencerapp.domain.model.Channel;
import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.EncryptedTokens;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.port.inbound.RegisterChannelUseCase;
import com.influencerapp.domain.port.outbound.ChannelRepository;
import com.influencerapp.infrastructure.adapter.security.TokenEncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
/**
 * Use case implementation orchestrating YouTube channel registration.
 *
 * @author judaro122
 * @since 1.0.0
 */


public class RegisterChannelUseCaseImpl implements RegisterChannelUseCase {

    private final ChannelRepository channelRepository;
    private final TokenEncryptionService tokenEncryptionService;

    @Override
    public ChannelId execute(TenantId tenantId, String authCode) {
        if (authCode == null || authCode.isBlank()) {
            throw new DomainException("Authorization code is required");
        }
        String channelId = UUID.randomUUID().toString();
        String youtubeChannelId = "youtube-channel-" + UUID.randomUUID();
        String youtubeChannelTitle = "Channel " + channelId;
        byte[] accessTokenEnc = tokenEncryptionService.encrypt("access-token-" + authCode);
        byte[] refreshTokenEnc = tokenEncryptionService.encrypt("refresh-token-" + authCode);
        String tokenExpiry = Instant.now().plusSeconds(3600).toString();
        Channel channel = new Channel(
                new ChannelId(channelId),
                tenantId,
                youtubeChannelId,
                youtubeChannelTitle,
                new EncryptedTokens(accessTokenEnc, refreshTokenEnc, tokenExpiry),
                "https://www.googleapis.com/auth/youtube.upload",
                Instant.now(),
                Instant.now()
        );
        channelRepository.save(channel);
        return new ChannelId(channelId);
    }
}

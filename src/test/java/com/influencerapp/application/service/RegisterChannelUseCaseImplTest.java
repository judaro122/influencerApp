package com.influencerapp.application.service;

import com.influencerapp.domain.exception.DomainException;
import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.EncryptedTokens;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.port.outbound.ChannelRepository;
import com.influencerapp.infrastructure.adapter.security.TokenEncryptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterChannelUseCase")
class RegisterChannelUseCaseImplTest {

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private TokenEncryptionService tokenEncryptionService;

    @InjectMocks
    private RegisterChannelUseCaseImpl useCase;

    @Test
    @DisplayName("should register channel and return channelId")
    void shouldRegisterChannel() {
        when(tokenEncryptionService.encrypt(any())).thenReturn("encrypted".getBytes());
        when(channelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ChannelId result = useCase.execute(new TenantId("tenant-1"), "auth-code");

        assertNotNull(result);
        verify(channelRepository).save(any());
        verify(tokenEncryptionService).encrypt("access-token-auth-code");
        verify(tokenEncryptionService).encrypt("refresh-token-auth-code");
    }

    @Test
    @DisplayName("should throw exception when auth code is blank")
    void shouldThrowWhenAuthCodeBlank() {
        assertThrows(DomainException.class, () -> useCase.execute(new TenantId("tenant-1"), ""));
        assertThrows(DomainException.class, () -> useCase.execute(new TenantId("tenant-1"), "   "));
    }
}

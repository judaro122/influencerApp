package com.influencerapp.application.service;

import com.influencerapp.domain.exception.DomainException;
import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.port.outbound.ChannelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterChannelUseCase")

/**
 * RegisterChannelUseCaseImplTest component.
 *
 * @author judaro122
 * @since 1.0.0
 */
class RegisterChannelUseCaseImplTest {

    @Mock
    private ChannelRepository channelRepository;

    @InjectMocks
    private RegisterChannelUseCaseImpl useCase;

    @Test
    @DisplayName("should register channel and return channelId")
    void shouldRegisterChannel() {
        when(channelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ChannelId result = useCase.execute(
                new TenantId("tenant-1"),
                "My Channel",
                "encrypted-access-token",
                "encrypted-refresh-token",
                "2025-12-31T23:59:59Z"
        );

        assertNotNull(result);
        verify(channelRepository).save(any());
    }

    @Test
    @DisplayName("should throw exception when name is blank")
    void shouldThrowWhenNameBlank() {
        assertThrows(DomainException.class, () -> useCase.execute(new TenantId("tenant-1"), "", "access", "refresh", "2025-12-31T23:59:59Z"));
        assertThrows(DomainException.class, () -> useCase.execute(new TenantId("tenant-1"), "   ", "access", "refresh", "2025-12-31T23:59:59Z"));
    }

    @Test
    @DisplayName("should throw exception when encrypted access token is blank")
    void shouldThrowWhenAccessTokenBlank() {
        assertThrows(DomainException.class, () -> useCase.execute(new TenantId("tenant-1"), "My Channel", "", "refresh", "2025-12-31T23:59:59Z"));
    }

    @Test
    @DisplayName("should throw exception when encrypted refresh token is blank")
    void shouldThrowWhenRefreshTokenBlank() {
        assertThrows(DomainException.class, () -> useCase.execute(new TenantId("tenant-1"), "My Channel", "access", "", "2025-12-31T23:59:59Z"));
    }

    @Test
    @DisplayName("should throw exception when token expiry is blank")
    void shouldThrowWhenTokenExpiryBlank() {
        assertThrows(DomainException.class, () -> useCase.execute(new TenantId("tenant-1"), "My Channel", "access", "refresh", ""));
    }
}

package com.influencerapp.application.service;

import com.influencerapp.domain.exception.DomainException;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.User;
import com.influencerapp.domain.port.outbound.TenantRepository;
import com.influencerapp.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterUserUseCase")
/**
 * RegisterUserUseCaseImplTest component.
 *
 * @author judaro122
 * @since 1.0.0
 */
class RegisterUserUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegisterUserUseCaseImpl useCase;

    @Test
    @DisplayName("should register user and return userId")
    void shouldRegisterUser() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(tenantRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String result = useCase.execute("test@example.com", "password123");

        assertNotNull(result);
        verify(tenantRepository).save(any());
        verify(userRepository).save(any());
        verify(passwordEncoder).encode("password123");
    }

    @Test
    @DisplayName("should throw exception when email is blank")
    void shouldThrowWhenEmailBlank() {
        assertThrows(DomainException.class, () -> useCase.execute("", "password123"));
        assertThrows(DomainException.class, () -> useCase.execute(null, "password123"));
        assertThrows(DomainException.class, () -> useCase.execute("   ", "password123"));
    }

    @Test
    @DisplayName("should throw exception when password is too short")
    void shouldThrowWhenPasswordTooShort() {
        assertThrows(DomainException.class, () -> useCase.execute("test@example.com", "short"));
    }

    @Test
    @DisplayName("should throw exception when email already registered")
    void shouldThrowWhenEmailExists() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(
                new User("user-1", new TenantId("tenant-1"), "test@example.com", "hash", Instant.now(), Instant.now())
        ));

        assertThrows(DomainException.class, () -> useCase.execute("test@example.com", "password123"));
    }
}

package com.influencerapp.application.service;

import com.influencerapp.domain.exception.DomainException;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.User;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginUseCase")
/**
 * LoginUseCaseImplTest component.
 *
 * @author judaro122
 * @since 1.0.0
 */
class LoginUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private LoginUseCaseImpl useCase;

    @Test
    @DisplayName("should return JWT token for valid credentials")
    void shouldReturnTokenForValidCredentials() throws Exception {
        java.lang.reflect.Field jwtSecretField = LoginUseCaseImpl.class.getDeclaredField("jwtSecret");
        jwtSecretField.setAccessible(true);
        jwtSecretField.set(useCase, "test-secret-key-for-jwt-signing-256-bits-long-enough");

        User user = new User(
                "user-1",
                new TenantId("tenant-1"),
                "test@example.com",
                "hashed-password",
                Instant.now(),
                Instant.now()
        );
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);

        String result = useCase.execute("test@example.com", "password123");

        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    @DisplayName("should throw exception when email not found")
    void shouldThrowWhenEmailNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> useCase.execute("unknown@example.com", "password123"));
    }

    @Test
    @DisplayName("should throw exception when password is wrong")
    void shouldThrowWhenWrongPassword() {
        User user = new User(
                "user-1",
                new TenantId("tenant-1"),
                "test@example.com",
                "hashed-password",
                Instant.now(),
                Instant.now()
        );
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "hashed-password")).thenReturn(false);

        assertThrows(DomainException.class, () -> useCase.execute("test@example.com", "wrongpassword"));
    }

    @Test
    @DisplayName("should throw exception when email is blank")
    void shouldThrowWhenEmailBlank() {
        assertThrows(DomainException.class, () -> useCase.execute("", "password123"));
        assertThrows(DomainException.class, () -> useCase.execute(null, "password123"));
    }

    @Test
    @DisplayName("should throw exception when password is blank")
    void shouldThrowWhenPasswordBlank() {
        assertThrows(DomainException.class, () -> useCase.execute("test@example.com", ""));
        assertThrows(DomainException.class, () -> useCase.execute("test@example.com", null));
    }
}
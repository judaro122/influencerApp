package com.influencerapp.infrastructure.repository;

import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class UserRepositoryImplTest extends RepositoryIntegrationTestBase {

    @Autowired
    private UserRepositoryImpl userRepository;

    @Test
    @DisplayName("should save and retrieve user")
    void shouldSaveAndRetrieveUser() {
        User user = new User("user-1", new TenantId("tenant-1"), "test@example.com", "hashed-password", Instant.now(), Instant.now());

        User saved = userRepository.save(user);
        Optional<User> found = userRepository.findById("user-1");

        assertTrue(found.isPresent());
        assertEquals("user-1", found.get().getUserId());
        assertEquals("test@example.com", found.get().getEmail());
        assertEquals("hashed-password", found.get().getPasswordHash());
    }

    @Test
    @DisplayName("should find user by email")
    void shouldFindByEmail() {
        User user = new User("user-1", new TenantId("tenant-1"), "test@example.com", "hashed-password", Instant.now(), Instant.now());
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertTrue(found.isPresent());
        assertEquals("user-1", found.get().getUserId());
    }

    @Test
    @DisplayName("should return empty when user not found")
    void shouldReturnEmptyWhenNotFound() {
        assertTrue(userRepository.findById("non-existent").isEmpty());
        assertTrue(userRepository.findByEmail("non-existent@example.com").isEmpty());
    }
}

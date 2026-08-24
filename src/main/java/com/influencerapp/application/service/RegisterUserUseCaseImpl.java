package com.influencerapp.application.service;

import com.influencerapp.domain.exception.DomainException;
import com.influencerapp.domain.model.Tenant;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.User;
import com.influencerapp.domain.port.inbound.RegisterUserUseCase;
import com.influencerapp.domain.port.outbound.TenantRepository;
import com.influencerapp.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Use case implementation orchestrating user registration.
 *
 * @author judaro122
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class RegisterUserUseCaseImpl implements RegisterUserUseCase {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String execute(String email, String password) {
        if (email == null || email.isBlank()) {
            throw new DomainException("Email is required");
        }
        if (password == null || password.length() < 8) {
            throw new DomainException("Password must be at least 8 characters");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new DomainException("Email already registered");
        }

        String userId = UUID.randomUUID().toString();
        TenantId tenantId = new TenantId(UUID.randomUUID().toString());
        String passwordHash = passwordEncoder.encode(password);
        Instant now = Instant.now();

        Tenant tenant = new Tenant(
                tenantId,
                email,
                now,
                now
        );
        tenantRepository.save(tenant);

        User user = new User(
                userId,
                tenantId,
                email,
                passwordHash,
                now,
                now
        );
        userRepository.save(user);
        return userId;
    }
}

package com.influencerapp.application.service;

import com.influencerapp.domain.exception.DomainException;
import com.influencerapp.domain.model.User;
import com.influencerapp.domain.port.inbound.LoginUseCase;
import com.influencerapp.domain.port.outbound.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
/**
 * Use case implementation orchestrating user authentication and JWT issuance.
 *
 * @author judaro122
 * @since 1.0.0
 */
public class LoginUseCaseImpl implements LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Override
    public String execute(String email, String password) {
        if (email == null || email.isBlank()) {
            throw new DomainException("Email is required");
        }
        if (password == null || password.isBlank()) {
            throw new DomainException("Password is required");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("Invalid credentials"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new DomainException("Invalid credentials");
        }
        return generateToken(user);
    }

    private String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtExpiration);
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Jwts.builder()
                .subject(user.getUserId())
                .claim("tenantId", user.getTenantId().getValue())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(Keys.hmacShaKeyFor(keyBytes))
                .compact();
    }
}
package com.influencerapp.infrastructure.adapter.http;

import com.influencerapp.application.dto.LoginRequest;
import com.influencerapp.application.dto.LoginResponse;
import com.influencerapp.application.dto.UserRegistrationRequest;
import com.influencerapp.domain.port.inbound.LoginUseCase;
import com.influencerapp.domain.port.inbound.RegisterUserUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;

    public AuthController(RegisterUserUseCase registerUserUseCase, LoginUseCase loginUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUseCase = loginUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody UserRegistrationRequest request) {
        registerUserUseCase.execute(request.getEmail(), request.getPassword());
        return ResponseEntity.created(URI.create("/api/auth/register")).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = loginUseCase.execute(request.getEmail(), request.getPassword());
        String tenantId = extractTenantIdFromToken(token);
        LoginResponse response = new LoginResponse();
        response.setAccessToken(token);
        response.setTenantId(tenantId);
        response.setExpiresIn(3600L);
        return ResponseEntity.ok(response);
    }

    private String extractTenantIdFromToken(String token) {
        // Extract tenantId from JWT claims
        // For now, parse the token to get the tenantId claim
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(payload);
                return node.get("tenantId").asText();
            }
        } catch (Exception e) {
            // If parsing fails, return a placeholder
        }
        return null;
    }
}

package com.influencerapp.infrastructure.adapter.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.influencerapp.application.dto.LoginRequest;
import com.influencerapp.application.dto.UserRegistrationRequest;
import com.influencerapp.domain.port.inbound.LoginUseCase;
import com.influencerapp.domain.port.inbound.RegisterUserUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Contract Tests")
class AuthControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegisterUserUseCase registerUserUseCase;

    @MockBean
    private LoginUseCase loginUseCase;

    @Test
    @DisplayName("POST /api/auth/register - should return 201 Created with valid request")
    void register_shouldReturn201() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "test@example.com",
                "password123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/auth/register")));
    }

    @Test
    @DisplayName("POST /api/auth/register - should return 400 with RFC 7807 Problem Detail for empty fields")
    void register_shouldReturn400WithEmptyFields() throws Exception {
        String invalidRequest = "{\"email\":\"\",\"password\":\"\"}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://api.influencerapp.com/problems/validation-error"))
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.email").value("Email is required"))
                .andExpect(jsonPath("$.fieldErrors.password").value(org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.equalTo("Password is required"),
                        org.hamcrest.Matchers.equalTo("Password must be at least 8 characters")
                )));
    }

    @Test
    @DisplayName("POST /api/auth/register - should return 400 with RFC 7807 Problem Detail for short password")
    void register_shouldReturn400WithShortPassword() throws Exception {
        String invalidRequest = "{\"email\":\"test@example.com\",\"password\":\"short\"}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://api.influencerapp.com/problems/validation-error"))
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.password").value("Password must be at least 8 characters"));
    }

    @Test
    @DisplayName("POST /api/auth/login - should return 200 with valid credentials")
    void login_shouldReturn200() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        String token = createTestJwtToken(tenantId);

        when(loginUseCase.execute(any(), any())).thenReturn(token);

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(token))
                .andExpect(jsonPath("$.tenantId").value(tenantId))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    private String createTestJwtToken(String tenantId) {
        return io.jsonwebtoken.Jwts.builder()
                .claim("sub", "test@example.com")
                .claim("tenantId", tenantId)
                .claim("exp", java.time.Instant.now().getEpochSecond() + 3600)
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor("test-secret-key-for-testing-only-1234567890".getBytes()))
                .compact();
    }

    @Test
    @DisplayName("POST /api/auth/login - should return 400 with RFC 7807 Problem Detail for invalid request")
    void login_shouldReturn400WithInvalidRequest() throws Exception {
        String invalidRequest = "{\"email\":\"\",\"password\":\"\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://api.influencerapp.com/problems/validation-error"))
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").exists());
    }
}

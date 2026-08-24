package com.influencerapp.infrastructure.adapter.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.influencerapp.application.dto.ChannelRegistrationRequest;
import com.influencerapp.application.dto.ChannelResponse;
import com.influencerapp.application.dto.PaginatedResponse;
import com.influencerapp.domain.model.Channel;
import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.EncryptedTokens;
import com.influencerapp.domain.model.PageResult;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.port.inbound.ListChannelsUseCase;
import com.influencerapp.domain.port.inbound.RegisterChannelUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChannelController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ChannelController Contract Tests")
class ChannelControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegisterChannelUseCase registerChannelUseCase;

    @MockBean
    private ListChannelsUseCase listChannelsUseCase;

    @Test
    @DisplayName("POST /api/channels/register - should return 201 Created with valid request")
    void registerChannel_shouldReturn201() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        ChannelId channelId = new ChannelId(UUID.randomUUID().toString());

        ChannelRegistrationRequest request = new ChannelRegistrationRequest();
        request.setName("My YouTube Channel");
        request.setEncryptedAccessToken("encrypted-access-token");
        request.setEncryptedRefreshToken("encrypted-refresh-token");
        request.setTokenExpiry("2025-12-31T23:59:59Z");

        when(registerChannelUseCase.execute(any(), any(), any(), any(), any())).thenReturn(channelId);

        mockMvc.perform(post("/api/channels/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("tenantId", new com.influencerapp.domain.model.TenantId(tenantId)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/channels/register")));
    }

    @Test
    @DisplayName("POST /api/channels/register - should return 400 with invalid request")
    void registerChannel_shouldReturn400WithInvalidRequest() throws Exception {
        String invalidRequest = "{\"name\":\"\",\"encryptedAccessToken\":\"\",\"encryptedRefreshToken\":\"\",\"tokenExpiry\":\"\"}";

        mockMvc.perform(post("/api/channels/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/channels - should return paginated list of channels")
    void listChannels_shouldReturnPaginatedList() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        Channel channel = new Channel(
                new ChannelId(UUID.randomUUID().toString()),
                new TenantId(tenantId),
                "UC" + UUID.randomUUID().toString().replace("-", ""),
                "My Channel",
                new EncryptedTokens(new byte[0], new byte[0], "2024-01-01T00:00:00Z"),
                "https://www.googleapis.com/auth/youtube.upload",
                Instant.now(),
                Instant.now()
        );

        PageResult<Channel> pageResult = new PageResult<>(
                List.of(channel),
                0,
                20,
                1,
                1
        );

        when(listChannelsUseCase.execute(any(TenantId.class), any(Integer.class), any(Integer.class))).thenReturn(pageResult);

        mockMvc.perform(get("/api/channels")
                        .param("page", "0")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON)
                        .requestAttr("tenantId", new com.influencerapp.domain.model.TenantId(tenantId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].youtubeChannelTitle").value("My Channel"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("GET /api/channels - should use default pagination parameters")
    void listChannels_shouldUseDefaultPagination() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        PageResult<Channel> pageResult = new PageResult<>(
                List.of(),
                0,
                20,
                0,
                0
        );

        when(listChannelsUseCase.execute(any(TenantId.class), any(Integer.class), any(Integer.class))).thenReturn(pageResult);

        mockMvc.perform(get("/api/channels")
                        .accept(MediaType.APPLICATION_JSON)
                        .requestAttr("tenantId", new com.influencerapp.domain.model.TenantId(tenantId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }
}

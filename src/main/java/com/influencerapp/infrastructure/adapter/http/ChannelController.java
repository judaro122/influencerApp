package com.influencerapp.infrastructure.adapter.http;

import com.influencerapp.application.dto.ChannelRegistrationRequest;
import com.influencerapp.application.dto.ChannelResponse;
import com.influencerapp.application.dto.PaginatedResponse;
import com.influencerapp.domain.model.Channel;
import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.PageResult;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.port.inbound.ListChannelsUseCase;
import com.influencerapp.domain.port.inbound.RegisterChannelUseCase;
import jakarta.validation.Valid;
import lombok.extern.java.Log;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/channels")
@Log
public class ChannelController {

    private final RegisterChannelUseCase registerChannelUseCase;
    private final ListChannelsUseCase listChannelsUseCase;

    public ChannelController(RegisterChannelUseCase registerChannelUseCase, ListChannelsUseCase listChannelsUseCase) {
        this.registerChannelUseCase = registerChannelUseCase;
        this.listChannelsUseCase = listChannelsUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> registerChannel(
            @Valid @RequestBody ChannelRegistrationRequest request,
            java.security.Principal principal) {
        String tenantId = extractTenantId(principal);
        log.warning("----------------registerChannelUseCase---------");
        registerChannelUseCase.execute(
                new TenantId(tenantId),
                request.getName(),
                request.getEncryptedAccessToken(),
                request.getEncryptedRefreshToken(),
                request.getTokenExpiry()
        );
        return ResponseEntity.created(URI.create("/api/channels/register")).build();
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<ChannelResponse>> listChannels(
            java.security.Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String tenantId = extractTenantId(principal);
        log.warning("----------------listChannels---------");
        PageResult<Channel> result = listChannelsUseCase.execute(new TenantId(tenantId), page, size);
        List<ChannelResponse> content = result.getContent().stream()
                .map(channel -> {
                    ChannelResponse response = new ChannelResponse();
                    response.setChannelId(channel.getChannelId());
                    response.setYoutubeChannelId(channel.getYoutubeChannelId());
                    response.setYoutubeChannelTitle(channel.getYoutubeChannelTitle());
                    response.setScope(channel.getScope());
                    response.setCreatedAt(channel.getCreatedAt());
                    return response;
                })
                .toList();
        PaginatedResponse<ChannelResponse> response = new PaginatedResponse<>(
                content,
                result.getPage(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
        return ResponseEntity.ok(response);
    }

    private String extractTenantId(java.security.Principal principal) {
        // tenantId is set as request attribute by JwtAuthFilter
        Object tenantIdAttr = org.springframework.web.context.request.RequestContextHolder
                .currentRequestAttributes()
                .getAttribute("tenantId", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
        if (tenantIdAttr instanceof com.influencerapp.domain.model.TenantId tenantId) {
            return tenantId.getValue();
        }
        throw new IllegalStateException("Tenant ID not found in request context");
    }
}

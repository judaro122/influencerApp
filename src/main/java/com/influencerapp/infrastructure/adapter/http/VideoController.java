package com.influencerapp.infrastructure.adapter.http;

import com.influencerapp.application.dto.PaginatedResponse;
import com.influencerapp.application.dto.VideoStatusResponse;
import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.PageResult;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.Video;
import com.influencerapp.domain.port.inbound.GetVideoStatusUseCase;
import com.influencerapp.domain.port.inbound.ListVideosUseCase;
import com.influencerapp.domain.port.inbound.UploadVideoUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.java.Log;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

/**
 * REST controller for video operations.
 *
 * @author judaro122
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/videos")
@Log
public class VideoController {

    private final UploadVideoUseCase uploadVideoUseCase;
    private final GetVideoStatusUseCase getVideoStatusUseCase;
    private final ListVideosUseCase listVideosUseCase;

    public VideoController(UploadVideoUseCase uploadVideoUseCase, GetVideoStatusUseCase getVideoStatusUseCase, ListVideosUseCase listVideosUseCase) {
        this.uploadVideoUseCase = uploadVideoUseCase;
        this.getVideoStatusUseCase = getVideoStatusUseCase;
        this.listVideosUseCase = listVideosUseCase;
    }

    @PostMapping("/upload")
    public ResponseEntity<VideoUploadResponse> uploadVideo(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid VideoUploadRequest request,
            java.security.Principal principal) {
        String tenantId = extractTenantId(principal);
        String channelId = request.getChannelId();

        com.influencerapp.domain.model.VideoId videoId = uploadVideoUseCase.execute(
                new TenantId(tenantId),
                request.getFile(),
                new ChannelId(channelId),
                idempotencyKey
        );

        VideoUploadResponse response = new VideoUploadResponse();
        response.setVideoId(videoId.getValue());
        return ResponseEntity.created(URI.create("/api/videos/" + videoId.getValue())).body(response);
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<VideoStatusResponse> getVideoStatus(
            @PathVariable String videoId,
            java.security.Principal principal) {
        String tenantId = extractTenantId(principal);
        com.influencerapp.domain.model.Video video = getVideoStatusUseCase.execute(
                new TenantId(tenantId),
                new com.influencerapp.domain.model.VideoId(videoId)
        );

        VideoStatusResponse response = new VideoStatusResponse();
        response.setVideoId(new com.influencerapp.domain.model.VideoId(video.getVideoId().getValue()));
        response.setTenantId(video.getTenantId());
        response.setChannelId(video.getChannelId());
        response.setStatus(video.getStatus());
        response.setMetadata(video.getMetadata());
        response.setYoutubeVideoId(video.getYoutubeVideoId());
        response.setYoutubeUrl(video.getYoutubeVideoId() != null ?
                "https://youtube.com/watch?v=" + video.getYoutubeVideoId().getValue() : null);
        response.setErrorReason(video.getErrorReason());
        response.setCreatedAt(video.getCreatedAt());
        response.setUpdatedAt(video.getUpdatedAt());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<VideoStatusResponse>> listVideos(
            java.security.Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String tenantId = extractTenantId(principal);
        PageResult<Video> result = listVideosUseCase.execute(new TenantId(tenantId), page, size);
        List<VideoStatusResponse> content = result.getContent().stream()
                .map(video -> {
                    VideoStatusResponse response = new VideoStatusResponse();
                    response.setVideoId(video.getVideoId());
                    response.setTenantId(video.getTenantId());
                    response.setChannelId(video.getChannelId());
                    response.setStatus(video.getStatus());
                    response.setMetadata(video.getMetadata());
                    response.setYoutubeVideoId(video.getYoutubeVideoId());
                    response.setYoutubeUrl(video.getYoutubeVideoId() != null ?
                            "https://youtube.com/watch?v=" + video.getYoutubeVideoId().getValue() : null);
                    response.setErrorReason(video.getErrorReason());
                    response.setCreatedAt(video.getCreatedAt());
                    response.setUpdatedAt(video.getUpdatedAt());
                    return response;
                })
                .toList();
        PaginatedResponse<VideoStatusResponse> response = new PaginatedResponse<>(
                content,
                result.getPage(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
        return ResponseEntity.ok(response);
    }

    private String extractTenantId(java.security.Principal principal) {
        Object tenantIdAttr = org.springframework.web.context.request.RequestContextHolder
                .currentRequestAttributes()
                .getAttribute("tenantId", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
        if (tenantIdAttr instanceof com.influencerapp.domain.model.TenantId tenantId) {
            return tenantId.getValue();
        }
        throw new IllegalStateException("Tenant ID not found in request context");
    }

    // DTOs
    public static class VideoUploadRequest {
        @NotNull
        private MultipartFile file;

        @NotBlank
        private String channelId;

        public MultipartFile getFile() { return file; }
        public void setFile(MultipartFile file) { this.file = file; }
        public String getChannelId() { return channelId; }
        public void setChannelId(String channelId) { this.channelId = channelId; }
    }

    public static class VideoUploadResponse {
        private String videoId;

        public String getVideoId() { return videoId; }
        public void setVideoId(String videoId) { this.videoId = videoId; }
    }
}

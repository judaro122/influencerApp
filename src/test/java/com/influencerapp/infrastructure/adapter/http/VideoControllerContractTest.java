package com.influencerapp.infrastructure.adapter.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.influencerapp.application.dto.VideoStatusResponse;
import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.FileMetadata;
import com.influencerapp.domain.model.PageResult;
import com.influencerapp.domain.model.StoragePath;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.Video;
import com.influencerapp.domain.model.VideoId;
import com.influencerapp.domain.model.VideoMetadata;
import com.influencerapp.domain.model.VideoStatus;
import com.influencerapp.domain.model.YouTubeVideoId;
import com.influencerapp.domain.port.inbound.GetVideoStatusUseCase;
import com.influencerapp.domain.port.inbound.ListVideosUseCase;
import com.influencerapp.domain.port.inbound.UploadVideoUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for {@link VideoController} validating against OpenAPI schema.
 *
 * @author judaro122
 * @since 1.0.0
 */
@WebMvcTest(VideoController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("VideoController Contract Tests")
class VideoControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UploadVideoUseCase uploadVideoUseCase;

    @MockBean
    private GetVideoStatusUseCase getVideoStatusUseCase;

    @MockBean
    private ListVideosUseCase listVideosUseCase;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    private TenantId tenantId;
    private VideoId videoId;
    private ChannelId channelId;

    @BeforeEach
    void setUp() {
        tenantId = new TenantId("tenant-123");
        videoId = new VideoId("video-" + UUID.randomUUID());
        channelId = new ChannelId("channel-456");
    }

    @Test
    @DisplayName("POST /api/videos/upload should return 201 with videoId")
    void shouldUploadVideoAndReturn201() throws Exception {
        when(uploadVideoUseCase.execute(any(), any(), any(), any())).thenReturn(videoId);

        MockMultipartFile file = new MockMultipartFile("file", "test-video.mp4", "video/mp4", "test content".getBytes());

        mockMvc.perform(multipart("/api/videos/upload")
                        .file(file)
                        .param("channelId", channelId.getValue())
                        .header("Idempotency-Key", "test-key-123")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .requestAttr("tenantId", tenantId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.videoId").value(videoId.getValue()));
    }

    @Test
    @DisplayName("GET /api/videos/{videoId} should return video status")
    void shouldGetVideoStatus() throws Exception {
        Video video = new Video(
                videoId,
                tenantId,
                channelId,
                new FileMetadata("test-video.mp4", 1024L, "video/mp4", "checksum"),
                new StoragePath("tenants/tenant-123/videos/" + videoId.getValue() + "/test-video.mp4"),
                new VideoMetadata(null, null),
                VideoStatus.PUBLISHED,
                new YouTubeVideoId("yt-123"),
                null,
                Instant.now(),
                Instant.now()
        );

        when(getVideoStatusUseCase.execute(any(), any())).thenReturn(video);

        mockMvc.perform(get("/api/videos/" + videoId.getValue())
                        .requestAttr("tenantId", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.videoId.value").value(videoId.getValue()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.youtubeUrl").value("https://youtube.com/watch?v=yt-123"));
    }

    @Test
    @DisplayName("GET /api/videos should return paginated list")
    void shouldListVideos() throws Exception {
        Video video = new Video(
                videoId,
                tenantId,
                channelId,
                new FileMetadata("test-video.mp4", 1024L, "video/mp4", "checksum"),
                new StoragePath("tenants/tenant-123/videos/" + videoId.getValue() + "/test-video.mp4"),
                new VideoMetadata(null, null),
                VideoStatus.RECEIVED,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        PageResult<Video> pageResult = new PageResult<>(List.of(video), 0, 20, 1, 1);

        when(listVideosUseCase.execute(any(), anyInt(), anyInt())).thenReturn(pageResult);

        mockMvc.perform(get("/api/videos")
                        .param("page", "0")
                        .param("size", "20")
                        .requestAttr("tenantId", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].videoId.value").value(videoId.getValue()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }
}

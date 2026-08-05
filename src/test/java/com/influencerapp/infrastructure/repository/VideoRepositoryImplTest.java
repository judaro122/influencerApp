package com.influencerapp.infrastructure.repository;

import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.FileMetadata;
import com.influencerapp.domain.model.StoragePath;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.Video;
import com.influencerapp.domain.model.VideoId;
import com.influencerapp.domain.model.VideoMetadata;
import com.influencerapp.domain.model.VideoStatus;
import com.influencerapp.domain.model.YouTubeVideoId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
/**
 * VideoRepositoryImplTest component.
 *
 * @author judaro122
 * @since 1.0.0
 */


class VideoRepositoryImplTest extends RepositoryIntegrationTestBase {

    @Autowired
    private VideoRepositoryImpl videoRepository;

    @Test
    @DisplayName("should save and retrieve video")
    void shouldSaveAndRetrieveVideo() {
        Video video = new Video(
                new VideoId("video-1"),
                new TenantId("tenant-1"),
                new ChannelId("channel-1"),
                new FileMetadata("test.mp4", 1024, "video/mp4", "checksum"),
                new StoragePath("tenants/tenant-1/videos/video-1/test.mp4"),
                new VideoMetadata("Title", "Description"),
                VideoStatus.RECEIVED,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        VideoId savedId = videoRepository.save(video).getVideoId();
        Video found = videoRepository.findByIdAndTenantId(savedId, new TenantId("tenant-1")).orElseThrow();

        assertEquals("video-1", found.getVideoId().getValue());
        assertEquals("tenant-1", found.getTenantId().getValue());
        assertEquals("channel-1", found.getChannelId().getValue());
        assertEquals("test.mp4", found.getFileMetadata().getFilename());
        assertEquals(VideoStatus.RECEIVED, found.getStatus());
    }

    @Test
    @DisplayName("should return empty when video not found")
    void shouldReturnEmptyWhenNotFound() {
        assertTrue(videoRepository.findByIdAndTenantId(new VideoId("non-existent"), new TenantId("tenant-1")).isEmpty());
    }

    @Test
    @DisplayName("should list videos by tenant with pagination")
    void shouldListVideosByTenant() {
        for (int i = 0; i < 5; i++) {
            Video video = new Video(
                    new VideoId("video-" + i),
                    new TenantId("tenant-1"),
                    new ChannelId("channel-1"),
                    new FileMetadata("test" + i + ".mp4", 1024, "video/mp4", "checksum"),
                    new StoragePath("tenants/tenant-1/videos/video-" + i + "/test" + i + ".mp4"),
                    new VideoMetadata("Title " + i, "Description " + i),
                    VideoStatus.RECEIVED,
                    null,
                    null,
                    Instant.now(),
                    Instant.now()
            );
            videoRepository.save(video);
        }

        var page = videoRepository.findByTenantId(new TenantId("tenant-1"), 0, 2);

        assertEquals(2, page.getContent().size());
        assertEquals(5, page.getTotalElements());
        assertEquals(3, page.getTotalPages());
    }
}

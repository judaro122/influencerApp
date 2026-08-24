package com.influencerapp.infrastructure.entity;

import com.influencerapp.domain.model.VideoStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "videos")
@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
/**
 * JPA entity mapping the video aggregate to the database schema.
 *
 * @author judaro122
 * @since 1.0.0
 */


public class VideoEntity {

    @Id
    @Column(name = "video_id", nullable = false, length = 255)
    private String videoId;

    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;

    @Column(name = "channel_id", nullable = false, length = 255)
    private String channelId;

    @Column(name = "filename", nullable = false, length = 500)
    private String filename;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type", nullable = false, length = 255)
    private String mimeType;

    @Column(name = "checksum", nullable = false, length = 255)
    private String checksum;

    @Column(name = "storage_path", nullable = false, length = 1000)
    private String storagePath;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private VideoStatus status;

    @Column(name = "youtube_video_id", length = 255)
    private String youtubeVideoId;

    @Column(name = "error_reason", columnDefinition = "text")
    private String errorReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

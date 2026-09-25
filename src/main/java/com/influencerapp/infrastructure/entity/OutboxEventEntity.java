package com.influencerapp.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * JPA entity mapping for outbox events.
 *
 * @author judaro122
 * @since 1.0.0
 */
@Entity
@Table(name = "outbox_events")
@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class OutboxEventEntity {

    public enum OutboxStatus {
        PENDING, PUBLISHED, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "aggregate_id", nullable = false, length = 255)
    private String aggregateId;

    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;

    @Column(name = "topic", nullable = false, length = 255)
    private String topic;

    @Column(name = "event_type", nullable = false, length = 255)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "schema_version", nullable = false, length = 20)
    private String schemaVersion;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;
}

package com.influencerapp.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * JPA entity mapping for processed events (consumer idempotency).
 *
 * @author judaro122
 * @since 1.0.0
 */
@Entity
@Table(name = "processed_events")
@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class ProcessedEventEntity {

    @Id
    @Column(name = "event_id", nullable = false, length = 255)
    private String eventId;

    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;

    @Column(name = "topic", nullable = false, length = 255)
    private String topic;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}

package com.influencerapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.Instant;

/**
 * Value object representing an outbox event pending Kafka publication.
 *
 * @author judaro122
 * @since 1.0.0
 */
@Value
@AllArgsConstructor
public class OutboxEvent {

    Long id;
    String aggregateId;
    TenantId tenantId;
    String topic;
    String eventType;
    String payload;
    String schemaVersion;
    String status;
    Integer retryCount;
    Instant createdAt;
    Instant publishedAt;
    String errorMessage;
}

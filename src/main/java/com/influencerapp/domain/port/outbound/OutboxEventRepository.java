package com.influencerapp.domain.port.outbound;

import com.influencerapp.domain.model.OutboxEvent;
import com.influencerapp.domain.model.TenantId;

import java.util.List;

/**
 * Outbound port defining the contract for outbox event persistence.
 *
 * @author judaro122
 * @since 1.0.0
 */
public interface OutboxEventRepository {

    void save(String aggregateId, TenantId tenantId, String topic, String eventType, String payload, String schemaVersion);

    List<OutboxEvent> findPendingEvents(int limit);

    void markAsPublished(Long id);

    void incrementRetryCount(Long id);

    void markAsFailed(Long id, String errorMessage);
}

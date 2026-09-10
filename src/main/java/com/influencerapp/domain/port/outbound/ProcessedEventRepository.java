package com.influencerapp.domain.port.outbound;

import com.influencerapp.domain.model.TenantId;

/**
 * Outbound port defining the contract for processed event idempotency tracking.
 *
 * @author judaro122
 * @since 1.0.0
 */
public interface ProcessedEventRepository {

    boolean existsByEventId(String eventId);

    int getRetryCount(String eventId);

    void save(String eventId, TenantId tenantId, String topic, int retryCount);
}

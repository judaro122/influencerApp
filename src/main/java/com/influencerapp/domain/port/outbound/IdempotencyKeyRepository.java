package com.influencerapp.domain.port.outbound;

import com.influencerapp.domain.model.TenantId;

import java.util.Optional;

/**
 * Outbound port defining the contract for idempotency key persistence.
 *
 * @author judaro122
 * @since 1.0.0
 */
public interface IdempotencyKeyRepository {

    boolean existsByIdempotencyKey(String idempotencyKey, TenantId tenantId);

    void save(String idempotencyKey, TenantId tenantId, String responseBody);
}

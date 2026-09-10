package com.influencerapp.infrastructure.repository;

import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.port.outbound.IdempotencyKeyRepository;
import com.influencerapp.infrastructure.entity.IdempotencyKeyEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Repository implementation for idempotency key persistence.
 *
 * @author judaro122
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class IdempotencyKeyRepositoryImpl implements IdempotencyKeyRepository {

    private final JpaIdempotencyKeyRepository jpaIdempotencyKeyRepository;

    @Override
    public boolean existsByIdempotencyKey(String idempotencyKey, TenantId tenantId) {
        return jpaIdempotencyKeyRepository.existsByIdempotencyKeyAndTenantId(idempotencyKey, tenantId.getValue());
    }

    @Override
    public void save(String idempotencyKey, TenantId tenantId, String responseBody) {
        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setIdempotencyKey(idempotencyKey);
        entity.setTenantId(tenantId.getValue());
        entity.setResponseBody(responseBody);
        entity.setCreatedAt(Instant.now());
        jpaIdempotencyKeyRepository.save(entity);
    }
}

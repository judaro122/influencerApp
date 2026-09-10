package com.influencerapp.infrastructure.repository;

import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.port.outbound.ProcessedEventRepository;
import com.influencerapp.infrastructure.entity.ProcessedEventEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Repository implementation for processed event idempotency tracking.
 *
 * @author judaro122
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class ProcessedEventRepositoryImpl implements ProcessedEventRepository {

    private final JpaProcessedEventRepository jpaProcessedEventRepository;

    @Override
    public boolean existsByEventId(String eventId) {
        return jpaProcessedEventRepository.existsByEventId(eventId);
    }

    @Override
    public int getRetryCount(String eventId) {
        ProcessedEventEntity entity = jpaProcessedEventRepository.findByEventId(eventId);
        return entity != null ? entity.getRetryCount() : 0;
    }

    @Override
    public void save(String eventId, TenantId tenantId, String topic, int retryCount) {
        ProcessedEventEntity entity = new ProcessedEventEntity();
        entity.setEventId(eventId);
        entity.setTenantId(tenantId.getValue());
        entity.setTopic(topic);
        entity.setRetryCount(retryCount);
        entity.setProcessedAt(Instant.now());
        jpaProcessedEventRepository.save(entity);
    }
}

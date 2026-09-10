package com.influencerapp.infrastructure.repository;

import com.influencerapp.domain.model.OutboxEvent;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.port.outbound.OutboxEventRepository;
import com.influencerapp.infrastructure.entity.OutboxEventEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Repository implementation for outbox event persistence.
 *
 * @author judaro122
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private final JpaOutboxEventRepository jpaOutboxEventRepository;

    @Override
    public void save(String aggregateId, TenantId tenantId, String topic, String eventType, String payload, String schemaVersion) {
        OutboxEventEntity entity = new OutboxEventEntity();
        entity.setAggregateId(aggregateId);
        entity.setTenantId(tenantId.getValue());
        entity.setTopic(topic);
        entity.setEventType(eventType);
        entity.setPayload(payload);
        entity.setSchemaVersion(schemaVersion);
        entity.setStatus(OutboxEventEntity.OutboxStatus.PENDING);
        entity.setRetryCount(0);
        entity.setCreatedAt(Instant.now());
        jpaOutboxEventRepository.save(entity);
    }

    @Override
    public List<OutboxEvent> findPendingEvents(int limit) {
        return jpaOutboxEventRepository.findByStatusOrderByIdAsc(OutboxEventEntity.OutboxStatus.PENDING)
                .stream()
                .limit(limit)
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void markAsPublished(Long id) {
        jpaOutboxEventRepository.findById(id).ifPresent(entity -> {
            entity.setStatus(OutboxEventEntity.OutboxStatus.PUBLISHED);
            entity.setPublishedAt(Instant.now());
            jpaOutboxEventRepository.save(entity);
        });
    }

    @Override
    public void markAsFailed(Long id, String errorMessage) {
        jpaOutboxEventRepository.findById(id).ifPresent(entity -> {
            entity.setStatus(OutboxEventEntity.OutboxStatus.FAILED);
            entity.setErrorMessage(errorMessage);
            jpaOutboxEventRepository.save(entity);
        });
    }

    @Override
    public void incrementRetryCount(Long id) {
        jpaOutboxEventRepository.incrementRetryCount(id);
    }

    private OutboxEvent toDomain(OutboxEventEntity entity) {
        return new OutboxEvent(
                entity.getId(),
                entity.getAggregateId(),
                new TenantId(entity.getTenantId()),
                entity.getTopic(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getSchemaVersion(),
                entity.getStatus().name(),
                entity.getRetryCount(),
                entity.getCreatedAt(),
                entity.getPublishedAt(),
                entity.getErrorMessage()
        );
    }
}

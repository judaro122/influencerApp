package com.influencerapp.infrastructure.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.port.outbound.KafkaProducerPort;
import com.influencerapp.domain.port.outbound.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Adapter implementing Kafka event publishing via the outbox pattern.
 * Events are persisted to the outbox table and published to Kafka by OutboxPublisher.
 *
 * @author judaro122
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VideoEventProducer implements KafkaProducerPort {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void send(String topic, Object payload) {
        try {
            String payloadJson = payload.toString();
            String aggregateId = extractAggregateId(payloadJson);
            String tenantId = extractTenantId(payloadJson);
            String eventType = topic;
            String schemaVersion = "1.0.0";

            outboxEventRepository.save(aggregateId, new TenantId(tenantId), topic, eventType, payloadJson, schemaVersion);
            log.debug("Saved event to outbox: topic={}, aggregateId={}", topic, aggregateId);
        } catch (Exception e) {
            log.error("Failed to save event to outbox: topic={}", topic, e);
            throw new RuntimeException("Failed to save event to outbox", e);
        }
    }

    private String extractAggregateId(String payloadJson) {
        try {
            Map<String, Object> map = objectMapper.readValue(payloadJson, Map.class);
            Object videoId = map.get("videoId");
            return videoId != null ? videoId.toString() : "unknown";
        } catch (Exception e) {
            log.warn("Failed to parse videoId from payload, using fallback", e);
            return "unknown";
        }
    }

    private String extractTenantId(String payloadJson) {
        try {
            Map<String, Object> map = objectMapper.readValue(payloadJson, Map.class);
            Object tenantId = map.get("tenantId");
            return tenantId != null ? tenantId.toString() : "unknown";
        } catch (Exception e) {
            log.warn("Failed to parse tenantId from payload, using fallback", e);
            return "unknown";
        }
    }
}

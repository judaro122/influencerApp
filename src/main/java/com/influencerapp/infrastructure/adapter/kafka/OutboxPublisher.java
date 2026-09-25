package com.influencerapp.infrastructure.adapter.kafka;

import com.influencerapp.domain.model.OutboxEvent;
import com.influencerapp.domain.port.outbound.KafkaProducerPort;
import com.influencerapp.domain.port.outbound.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled component that polls the outbox table and publishes events to Kafka.
 * Handles retry logic and marks events as published or failed.
 *
 * @author judaro122
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final int MAX_RETRIES = 3;
    private static final String DLQ_TOPIC_SUFFIX = "-dlq";

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents(10);
        for (OutboxEvent event : pendingEvents) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getId().toString(), event.getPayload());
                outboxEventRepository.markAsPublished(event.getId());
                log.debug("Published outbox event id={} to topic={}", event.getId(), event.getTopic());
            } catch (Exception e) {
                log.error("Failed to publish outbox event id={} to topic={}", event.getId(), event.getTopic(), e);
                outboxEventRepository.incrementRetryCount(event.getId());
                int retryCount = event.getRetryCount() + 1;

                if (retryCount >= MAX_RETRIES) {
                    log.error("Outbox event id={} exceeded max retries ({}), marking as failed", event.getId(), MAX_RETRIES);
                    String dlqTopic = event.getTopic() + DLQ_TOPIC_SUFFIX;
                    kafkaTemplate.send(dlqTopic, event.getId().toString(), event.getPayload());
                    outboxEventRepository.markAsFailed(event.getId(), "Max retries exceeded: " + e.getMessage());
                } else {
                    log.warn("Outbox event id={} will be retried (attempt {}/{})", event.getId(), retryCount, MAX_RETRIES);
                }
            }
        }
    }
}

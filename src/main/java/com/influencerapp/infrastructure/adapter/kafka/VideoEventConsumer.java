package com.influencerapp.infrastructure.adapter.kafka;

import com.influencerapp.application.service.VideoProcessingService;
import com.influencerapp.domain.exception.DomainException;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.VideoId;
import com.influencerapp.domain.port.outbound.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka consumer for video events with idempotency and DLQ routing.
 *
 * @author judaro122
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VideoEventConsumer {

    private static final int MAX_RETRIES = 3;
    private static final String DLQ_TOPIC_SUFFIX = "-dlq";

    private final VideoProcessingService videoProcessingService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "video-received", groupId = "influencerapp-group")
    public void handleVideoReceived(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        String eventId = key;
        if (processedEventRepository.existsByEventId(eventId)) {
            log.debug("Skipping already processed event: {}", eventId);
            return;
        }

        try {
            Map<String, Object> event = objectMapper.readValue(payload, Map.class);
            String tenantId = (String) event.get("tenantId");
            String videoId = (String) event.get("videoId");

            log.info("Processing video-received event: videoId={}, tenantId={}", videoId, tenantId);

            videoProcessingService.processVideo(new TenantId(tenantId), new VideoId(videoId));

            processedEventRepository.save(eventId, new TenantId(tenantId), topic, 0);
            log.info("Successfully processed video-received event: {}", eventId);
        } catch (Exception e) {
            log.error("Failed to process video-received event: {}", eventId, e);
            handleProcessingFailure(eventId, topic, payload, e);
        }
    }

    private void handleProcessingFailure(String eventId, String topic, String payload, Exception e) {
        int retryCount = processedEventRepository.getRetryCount(eventId) + 1;

        if (retryCount >= MAX_RETRIES) {
            log.error("Event {} exceeded max retries ({}), routing to DLQ", eventId, MAX_RETRIES);
            String dlqTopic = topic + DLQ_TOPIC_SUFFIX;
            kafkaTemplate.send(dlqTopic, eventId, payload);
            processedEventRepository.save(eventId, new TenantId("unknown"), topic, retryCount);
        } else {
            log.warn("Event {} failed, will retry (attempt {}/{})", eventId, retryCount, MAX_RETRIES);
            processedEventRepository.save(eventId, new TenantId("unknown"), topic, retryCount);
            throw new DomainException("Failed to process event after " + retryCount + " attempts: " + e.getMessage());
        }
    }
}

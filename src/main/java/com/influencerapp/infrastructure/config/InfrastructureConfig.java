package com.influencerapp.infrastructure.config;

import com.influencerapp.domain.port.outbound.*;
import com.influencerapp.infrastructure.adapter.ai.GeminiTextGenerationAdapter;
import com.influencerapp.infrastructure.adapter.kafka.OutboxPublisher;
import com.influencerapp.infrastructure.adapter.kafka.VideoEventProducer;
import com.influencerapp.infrastructure.adapter.storage.MinioStorageAdapter;
import com.influencerapp.infrastructure.repository.*;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Spring configuration class wiring infrastructure adapters and ports.
 *
 * @author judaro122
 * @since 1.0.0
 */
@Configuration
public class InfrastructureConfig {

    @Bean
    public VideoRepository videoRepository(VideoRepositoryImpl videoRepositoryImpl) {
        return videoRepositoryImpl;
    }

    @Bean
    public ChannelRepository channelRepository(ChannelRepositoryImpl channelRepositoryImpl) {
        return channelRepositoryImpl;
    }

    @Bean
    public UserRepository userRepository(UserRepositoryImpl userRepositoryImpl) {
        return userRepositoryImpl;
    }

    @Bean
    public TenantRepository tenantRepository(TenantRepositoryImpl tenantRepositoryImpl) {
        return tenantRepositoryImpl;
    }

    @Bean
    public IdempotencyKeyRepository idempotencyKeyRepository(IdempotencyKeyRepositoryImpl idempotencyKeyRepositoryImpl) {
        return idempotencyKeyRepositoryImpl;
    }

    @Bean
    public OutboxEventRepository outboxEventRepository(OutboxEventRepositoryImpl outboxEventRepositoryImpl) {
        return outboxEventRepositoryImpl;
    }

    @Bean
    public ProcessedEventRepository processedEventRepository(ProcessedEventRepositoryImpl processedEventRepositoryImpl) {
        return processedEventRepositoryImpl;
    }

    @Bean
    public ObjectStoragePort objectStoragePort(MinioStorageAdapter minioStorageAdapter) {
        return minioStorageAdapter;
    }

    @Bean
    public AITextGenerationPort aiTextGenerationPort(GeminiTextGenerationAdapter geminiTextGenerationAdapter) {
        return geminiTextGenerationAdapter;
    }

    @Bean
    public KafkaProducerPort kafkaProducerPort(VideoEventProducer videoEventProducer) {
        return videoEventProducer;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CircuitBreaker circuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(10)
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
        return CircuitBreaker.of("gemini", config);
    }

    @Bean
    public Retry retry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .retryOnException(e -> e instanceof RuntimeException)
                .build();
        return Retry.of("gemini", config);
    }

    @Bean
    public TimeLimiter timeLimiter() {
        return TimeLimiter.of(Duration.ofSeconds(10));
    }

    @Bean
    public MinioClient minioClient(
            @Value("${spring.minio.endpoint:http://minio:9000}") String endpoint,
            @Value("${spring.minio.access-key:minioadmin}") String accessKey,
            @Value("${spring.minio.secret-key:minioadmin}") String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    public OutboxPublisher outboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, Object> kafkaTemplate) {
        return new OutboxPublisher(outboxEventRepository, kafkaTemplate);
    }
}

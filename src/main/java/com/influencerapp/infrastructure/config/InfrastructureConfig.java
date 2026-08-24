package com.influencerapp.infrastructure.config;

import com.influencerapp.domain.port.outbound.AITextGenerationPort;
import com.influencerapp.domain.port.outbound.ChannelRepository;
import com.influencerapp.domain.port.outbound.KafkaProducerPort;
import com.influencerapp.domain.port.outbound.ObjectStoragePort;
import com.influencerapp.domain.port.outbound.TenantRepository;
import com.influencerapp.domain.port.outbound.UserRepository;
import com.influencerapp.domain.port.outbound.VideoRepository;
import com.influencerapp.domain.port.outbound.YouTubeUploadPort;
import com.influencerapp.infrastructure.adapter.ai.GeminiTextGenerationAdapter;
import com.influencerapp.infrastructure.adapter.kafka.KafkaProducerAdapter;
import com.influencerapp.infrastructure.adapter.storage.MinioStorageAdapter;
import com.influencerapp.infrastructure.adapter.youtube.YouTubeUploadAdapter;
import com.influencerapp.infrastructure.repository.ChannelRepositoryImpl;
import com.influencerapp.infrastructure.repository.TenantRepositoryImpl;
import com.influencerapp.infrastructure.repository.UserRepositoryImpl;
import com.influencerapp.infrastructure.repository.VideoRepositoryImpl;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
/**
 * Spring configuration class wiring infrastructure adapters and ports.
 *
 * @author judaro122
 * @since 1.0.0
 */


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
    public ObjectStoragePort objectStoragePort(MinioStorageAdapter minioStorageAdapter) {
        return minioStorageAdapter;
    }

    @Bean
    public YouTubeUploadPort youtubeUploadPort(YouTubeUploadAdapter youtubeUploadAdapter) {
        return youtubeUploadAdapter;
    }

    @Bean
    public AITextGenerationPort aiTextGenerationPort(GeminiTextGenerationAdapter geminiTextGenerationAdapter) {
        return geminiTextGenerationAdapter;
    }

    @Bean
    public KafkaProducerPort kafkaProducerPort(KafkaProducerAdapter kafkaProducerAdapter) {
        return kafkaProducerAdapter;
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
            @Value("${minio.endpoint}") String endpoint,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}

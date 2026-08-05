package com.influencerapp.infrastructure.config;

import com.influencerapp.domain.port.outbound.AITextGenerationPort;
import com.influencerapp.domain.port.outbound.ChannelRepository;
import com.influencerapp.domain.port.outbound.KafkaProducerPort;
import com.influencerapp.domain.port.outbound.ObjectStoragePort;
import com.influencerapp.domain.port.outbound.UserRepository;
import com.influencerapp.domain.port.outbound.VideoRepository;
import com.influencerapp.domain.port.outbound.YouTubeUploadPort;
import com.influencerapp.infrastructure.adapter.ai.GeminiTextGenerationAdapter;
import com.influencerapp.infrastructure.adapter.kafka.KafkaProducerAdapter;
import com.influencerapp.infrastructure.adapter.storage.MinioStorageAdapter;
import com.influencerapp.infrastructure.adapter.youtube.YouTubeUploadAdapter;
import com.influencerapp.infrastructure.repository.ChannelRepositoryImpl;
import com.influencerapp.infrastructure.repository.UserRepositoryImpl;
import com.influencerapp.infrastructure.repository.VideoRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}

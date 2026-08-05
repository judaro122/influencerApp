package com.influencerapp.infrastructure.repository;

import com.influencerapp.domain.model.*;
import com.influencerapp.domain.port.outbound.ChannelRepository;
import com.influencerapp.infrastructure.entity.ChannelEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor

/**
 * Repository implementation persisting channel aggregates via JPA.
 *
 * @author judaro122
 * @since 1.0.0
 */
public class ChannelRepositoryImpl implements ChannelRepository {

    private final JpaChannelRepository jpaChannelRepository;

    @Override
    public Channel save(Channel channel) {
        ChannelEntity entity = toEntity(channel);
        ChannelEntity saved = jpaChannelRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Channel> findByIdAndTenantId(ChannelId channelId, TenantId tenantId) {
        return jpaChannelRepository.findByChannelIdAndTenantId(channelId.getValue(), tenantId.getValue())
                .map(this::toDomain);
    }

    @Override
    public PageResult<Channel> findByTenantId(TenantId tenantId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ChannelEntity> entityPage = jpaChannelRepository.findByTenantId(tenantId.getValue(), pageRequest);
        return new PageResult<>(
                entityPage.getContent().stream().map(this::toDomain).toList(),
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages()
        );
    }

    private Channel toDomain(ChannelEntity entity) {
        return new Channel(
                new ChannelId(entity.getChannelId()),
                new TenantId(entity.getTenantId()),
                entity.getYoutubeChannelId(),
                entity.getYoutubeChannelTitle(),
                new EncryptedTokens(
                        entity.getAccessTokenEnc(),
                        entity.getRefreshTokenEnc(),
                        entity.getTokenExpiry().toString()
                ),
                entity.getScope(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ChannelEntity toEntity(Channel channel) {
        ChannelEntity entity = new ChannelEntity();
        entity.setChannelId(channel.getChannelId().getValue());
        entity.setTenantId(channel.getTenantId().getValue());
        entity.setYoutubeChannelId(channel.getYoutubeChannelId());
        entity.setYoutubeChannelTitle(channel.getYoutubeChannelTitle());
        entity.setAccessTokenEnc(channel.getEncryptedTokens().getAccessToken());
        entity.setRefreshTokenEnc(channel.getEncryptedTokens().getRefreshToken());
        entity.setTokenExpiry(Instant.parse(channel.getEncryptedTokens().getTokenExpiry()));
        entity.setScope(channel.getScope());
        entity.setCreatedAt(channel.getCreatedAt());
        entity.setUpdatedAt(channel.getUpdatedAt());
        return entity;
    }
}

package com.influencerapp.infrastructure.repository;

import com.influencerapp.domain.model.Channel;
import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.infrastructure.entity.ChannelEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaChannelRepository extends JpaRepository<ChannelEntity, String> {

    Optional<ChannelEntity> findByChannelIdAndTenantId(String channelId, String tenantId);

    Page<ChannelEntity> findByTenantId(String tenantId, Pageable pageable);
}

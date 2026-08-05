package com.influencerapp.infrastructure.repository;

import com.influencerapp.infrastructure.entity.ChannelEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


/**
 * Spring Data JPA repository interface for channel entities.
 *
 * @author judaro122
 * @since 1.0.0
 */
public interface JpaChannelRepository extends JpaRepository<ChannelEntity, String> {

    Optional<ChannelEntity> findByChannelIdAndTenantId(String channelId, String tenantId);

    Page<ChannelEntity> findByTenantId(String tenantId, Pageable pageable);
}

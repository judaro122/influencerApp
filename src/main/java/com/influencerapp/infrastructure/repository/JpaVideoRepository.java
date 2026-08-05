package com.influencerapp.infrastructure.repository;

import com.influencerapp.infrastructure.entity.VideoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


/**
 * Spring Data JPA repository interface for video entities.
 *
 * @author judaro122
 * @since 1.0.0
 */
public interface JpaVideoRepository extends JpaRepository<VideoEntity, String> {

    Optional<VideoEntity> findByVideoIdAndTenantId(String videoId, String tenantId);

    Page<VideoEntity> findByTenantId(String tenantId, Pageable pageable);
}

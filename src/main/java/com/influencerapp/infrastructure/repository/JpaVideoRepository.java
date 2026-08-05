package com.influencerapp.infrastructure.repository;

import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.model.Video;
import com.influencerapp.domain.model.VideoId;
import com.influencerapp.infrastructure.entity.VideoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaVideoRepository extends JpaRepository<VideoEntity, String> {

    Optional<VideoEntity> findByVideoIdAndTenantId(String videoId, String tenantId);

    Page<VideoEntity> findByTenantId(String tenantId, Pageable pageable);
}

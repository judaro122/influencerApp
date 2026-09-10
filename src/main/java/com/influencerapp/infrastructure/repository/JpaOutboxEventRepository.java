package com.influencerapp.infrastructure.repository;

import com.influencerapp.infrastructure.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository interface for outbox event entities.
 *
 * @author judaro122
 * @since 1.0.0
 */
public interface JpaOutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    List<OutboxEventEntity> findByStatusOrderByIdAsc(OutboxEventEntity.OutboxStatus status);

    @Modifying
    @Query("UPDATE OutboxEventEntity e SET e.retryCount = e.retryCount + 1 WHERE e.id = :id")
    int incrementRetryCount(@Param("id") Long id);
}

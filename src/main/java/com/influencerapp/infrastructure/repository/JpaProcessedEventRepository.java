package com.influencerapp.infrastructure.repository;

import com.influencerapp.infrastructure.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository interface for processed event entities.
 *
 * @author judaro122
 * @since 1.0.0
 */
public interface JpaProcessedEventRepository extends JpaRepository<ProcessedEventEntity, String> {

    boolean existsByEventId(String eventId);

    ProcessedEventEntity findByEventId(String eventId);
}

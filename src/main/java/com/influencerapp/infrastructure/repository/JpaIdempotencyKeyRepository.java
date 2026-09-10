package com.influencerapp.infrastructure.repository;

import com.influencerapp.infrastructure.entity.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository interface for idempotency key entities.
 *
 * @author judaro122
 * @since 1.0.0
 */
public interface JpaIdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, String> {

    boolean existsByIdempotencyKeyAndTenantId(String idempotencyKey, String tenantId);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<IdempotencyKeyEntity> findByIdempotencyKey(String idempotencyKey);
}

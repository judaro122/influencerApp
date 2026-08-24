package com.influencerapp.infrastructure.repository;

import com.influencerapp.infrastructure.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository interface for tenant entities.
 *
 * @author judaro122
 * @since 1.0.0
 */
public interface JpaTenantRepository extends JpaRepository<TenantEntity, String> {

    boolean existsById(String id);
}

package com.influencerapp.infrastructure.repository;

import com.influencerapp.domain.model.Tenant;
import com.influencerapp.domain.model.TenantId;
import com.influencerapp.domain.port.outbound.TenantRepository;
import com.influencerapp.infrastructure.entity.TenantEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Repository implementation persisting tenant aggregates via JPA.
 *
 * @author judaro122
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class TenantRepositoryImpl implements TenantRepository {

    private final JpaTenantRepository jpaTenantRepository;

    @Override
    public Tenant save(Tenant tenant) {
        TenantEntity entity = toEntity(tenant);
        TenantEntity saved = jpaTenantRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public boolean existsById(TenantId tenantId) {
        return jpaTenantRepository.existsById(tenantId.getValue());
    }

    private Tenant toDomain(TenantEntity entity) {
        return new Tenant(
                new TenantId(entity.getId()),
                entity.getId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private TenantEntity toEntity(Tenant tenant) {
        TenantEntity entity = new TenantEntity();
        entity.setId(tenant.getTenantId().getValue());
        entity.setCreatedAt(tenant.getCreatedAt());
        entity.setUpdatedAt(tenant.getUpdatedAt());
        return entity;
    }
}

package com.influencerapp.domain.port.outbound;

import com.influencerapp.domain.model.Tenant;
import com.influencerapp.domain.model.TenantId;

/**
 * Outbound port defining the contract for tenant persistence operations.
 *
 * @author judaro122
 * @since 1.0.0
 */
public interface TenantRepository {

    Tenant save(Tenant tenant);

    boolean existsById(TenantId tenantId);
}

package com.influencerapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Value;
import java.time.Instant;

@Value
@AllArgsConstructor
/**
 * Aggregate root representing a tenant in the multi-tenant InfluencerAPP system.
 *
 * @author judaro122
 * @since 1.0.0
 */


public class Tenant {

    TenantId tenantId;
    String name;
    Instant createdAt;
    Instant updatedAt;
}
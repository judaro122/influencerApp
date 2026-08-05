package com.influencerapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Value;
import java.time.Instant;

@Value
@AllArgsConstructor
public class Tenant {

    TenantId tenantId;
    String name;
    Instant createdAt;
    Instant updatedAt;
}
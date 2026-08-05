package com.influencerapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Value;
import java.time.Instant;

@Value
@AllArgsConstructor
public class User {

    String userId;
    TenantId tenantId;
    String email;
    String passwordHash;
    Instant createdAt;
    Instant updatedAt;
}
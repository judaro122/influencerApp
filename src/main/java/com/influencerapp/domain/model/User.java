package com.influencerapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Value;
import java.time.Instant;

@Value
@AllArgsConstructor
/**
 * Entity representing a registered user within the InfluencerAPP system.
 *
 * @author judaro122
 * @since 1.0.0
 */


public class User {

    String userId;
    TenantId tenantId;
    String email;
    String passwordHash;
    Instant createdAt;
    Instant updatedAt;
}
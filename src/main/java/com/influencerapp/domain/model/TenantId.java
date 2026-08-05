package com.influencerapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Value;



@Value
@AllArgsConstructor
/**
 * Immutable value object representing a tenant identifier within the multi-tenant InfluencerAPP system.
 *
 * @author judaro122
 * @since 1.0.0
 */
public class TenantId {

    
    String value;
}

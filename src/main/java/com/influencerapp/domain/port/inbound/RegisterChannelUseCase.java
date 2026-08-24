package com.influencerapp.domain.port.inbound;

import com.influencerapp.domain.model.ChannelId;
import com.influencerapp.domain.model.TenantId;
/**
 * Inbound port defining the contract for YouTube channel registration.
 *
 * @author judaro122
 * @since 1.0.0
 */
public interface RegisterChannelUseCase {

    ChannelId execute(
            TenantId tenantId,
            String name,
            String encryptedAccessToken,
            String encryptedRefreshToken,
            String tokenExpiry
    );
}

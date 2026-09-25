package com.influencerapp.test.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelRegistrationRequest {
    private String name;
    private String encryptedAccessToken;
    private String encryptedRefreshToken;
    private String tokenExpiry;
}

package com.influencerapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
/**
 * Immutable value object holding encrypted OAuth2 tokens for YouTube API access.
 *
 * @author judaro122
 * @since 1.0.0
 */


public class EncryptedTokens {

    byte[] accessToken;
    byte[] refreshToken;
    String tokenExpiry;
}
package com.influencerapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class EncryptedTokens {

    byte[] accessToken;
    byte[] refreshToken;
    String tokenExpiry;
}
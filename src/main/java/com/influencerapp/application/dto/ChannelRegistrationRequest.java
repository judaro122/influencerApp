package com.influencerapp.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data transfer object carrying YouTube channel registration parameters.
 *
 * @author judaro122
 * @since 1.0.0
 */
@Data
public class ChannelRegistrationRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String encryptedAccessToken;

    @NotBlank
    private String encryptedRefreshToken;

    @NotBlank
    private String tokenExpiry;
}

package com.influencerapp.application.dto;

import lombok.Data;

/**
 * Data transfer object for login responses.
 *
 * @author judaro122
 * @since 1.0.0
 */
@Data
public class LoginResponse {

    private String accessToken;
    private String tenantId;
    private long expiresIn;
}

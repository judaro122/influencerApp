package com.influencerapp.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
/**
 * Data transfer object carrying YouTube channel registration parameters.
 *
 * @author judaro122
 * @since 1.0.0
 */


public class ChannelRegistrationRequest {

    @NotBlank
    private String authCode;
}
package com.influencerapp.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChannelRegistrationRequest {

    @NotBlank
    private String authCode;
}
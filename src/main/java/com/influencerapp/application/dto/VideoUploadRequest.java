package com.influencerapp.application.dto;

import com.influencerapp.domain.model.ChannelId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class VideoUploadRequest {

    @NotNull
    private MultipartFile file;

    @NotBlank
    private String channelId;
}
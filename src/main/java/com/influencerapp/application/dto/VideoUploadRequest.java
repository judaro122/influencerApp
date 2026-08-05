package com.influencerapp.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
/**
 * Data transfer object carrying video upload request parameters.
 *
 * @author judaro122
 * @since 1.0.0
 */
public class VideoUploadRequest {

    @NotNull
    private MultipartFile file;

    @NotBlank
    private String channelId;
}
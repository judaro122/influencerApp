package com.influencerapp.application.dto;

import com.influencerapp.domain.model.ChannelId;
import lombok.Data;
import java.time.Instant;

@Data
/**
 * Data transfer object representing a YouTube channel summary.
 *
 * @author judaro122
 * @since 1.0.0
 */


public class ChannelResponse {

    private ChannelId channelId;
    private String youtubeChannelId;
    private String youtubeChannelTitle;
    private String scope;
    private Instant createdAt;
}
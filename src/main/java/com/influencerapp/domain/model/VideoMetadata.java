package com.influencerapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
/**
 * Immutable value object holding AI-generated or placeholder title and description.
 *
 * @author judaro122
 * @since 1.0.0
 */


public class VideoMetadata {

    String title;
    String description;
}
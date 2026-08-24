package com.influencerapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
/**
 * Immutable value object encapsulating metadata about an uploaded video file.
 *
 * @author judaro122
 * @since 1.0.0
 */


public class FileMetadata {

    String filename;
    long size;
    String mimeType;
    String checksum;
}
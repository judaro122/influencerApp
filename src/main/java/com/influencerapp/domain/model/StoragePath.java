package com.influencerapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
/**
 * Immutable value object representing a tenant-isolated storage path for video files.
 *
 * @author judaro122
 * @since 1.0.0
 */


public class StoragePath {

    String value;
}
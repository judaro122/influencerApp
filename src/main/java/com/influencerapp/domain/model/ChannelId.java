package com.influencerapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
/**
 * Immutable value object representing a unique YouTube channel identifier.
 *
 * @author judaro122
 * @since 1.0.0
 */


public class ChannelId {

    String value;
}